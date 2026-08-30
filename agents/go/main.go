package main

import (
	"bytes"
	"context"
	"encoding/json"
	"flag"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"os/signal"
	"runtime"
	"strings"
	"syscall"
	"time"
)

var httpClient = &http.Client{
	Timeout: 30 * time.Second,
}

func main() {
	if len(os.Args) < 2 {
		printUsage()
		os.Exit(1)
	}

	command := os.Args[1]
	switch command {
	case "register":
		runRegister(os.Args[2:])
	case "run":
		runAgent(os.Args[2:])
	default:
		printUsage()
		os.Exit(1)
	}
}

const (
	colorReset   = "\033[0m"
	colorBold    = "\033[1m"
	colorBrand   = "\033[38;2;124;58;237m"
	colorPurple  = "\033[38;2;167;139;250m"
	colorGreen   = "\033[38;2;16;185;129m"
	colorBlue    = "\033[38;2;59;130;246m"
	colorRed     = "\033[38;2;239;68;68m"
	colorAmber   = "\033[38;2;245;158;11m"
	colorMuted   = "\033[38;2;113;113;122m"
	colorDim     = "\033[2m"
)

func printBanner() {
	banner := `
` + colorBrand + colorBold + `  ███████╗ ██████╗ ██╗   ██╗██╗██╗  ██╗
  ╚══███╔╝██╔═══██╗██║   ██║██║██║ ██╔╝
    ███╔╝ ██║   ██║██║   ██║██║█████╔╝ 
   ███╔╝  ██║   ██║╚██╗ ██╔╝██║██╔═██╗ 
  ███████╗╚██████╔╝ ╚████╔╝ ██║██║  ██╗
  ╚══════╝ ╚═════╝   ╚═══╝  ╚═╝╚═╝  ╚═╝` + colorReset + `

  ` + colorPurple + colorBold + `:: Zovik Distributed Pipeline Engine ::` + colorReset + ` ` + colorDim + `(Runner v1.0.0)` + colorReset + `
`
	fmt.Print(banner)
}

func printUsage() {
	printBanner()
	fmt.Println(colorBold + "Usage:" + colorReset)
	fmt.Println("  zovik-agent register --url <server_url> --token <registration_token> [--name <name>]")
	fmt.Println("  zovik-agent run [--config config.yaml]")
}

func runRegister(args []string) {
	fs := flag.NewFlagSet("register", flag.ExitOnError)
	urlFlag := fs.String("url", "http://localhost:8080", "Zovik Master server URL")
	tokenFlag := fs.String("token", "", "One-time registration token (rb_reg_...)")
	nameFlag := fs.String("name", "", "Runner display name")
	configPathFlag := fs.String("config", "config.yaml", "Path to config file")

	_ = fs.Parse(args)

	printBanner()

	if *tokenFlag == "" {
		log.Fatalf(colorRed+"[✕] Error: --token is required. Generate one in Zovik Web UI first."+colorReset)
	}

	hostname, _ := os.Hostname()
	runnerName := *nameFlag
	if runnerName == "" {
		runnerName = fmt.Sprintf("runner-%s", hostname)
	}

	regPayload := map[string]interface{}{
		"registrationToken": *tokenFlag,
		"name":              runnerName,
		"os":                runtime.GOOS,
		"cpuCores":          runtime.NumCPU(),
		"memoryBytes":       16 * 1024 * 1024 * 1024, // Fallback default
	}

	bodyBytes, _ := json.Marshal(regPayload)
	endpoint := fmt.Sprintf("%s/api/v1/runners/register", strings.TrimRight(*urlFlag, "/"))

	resp, err := http.Post(endpoint, "application/json", bytes.NewReader(bodyBytes))
	if err != nil {
		log.Fatalf(colorRed+"[✕] Failed to connect to Zovik master: %v"+colorReset, err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(resp.Body)
		log.Fatalf(colorRed+"[✕] Registration failed (HTTP %d): %s"+colorReset, resp.StatusCode, string(body))
	}

	var apiResp struct {
		Success bool `json:"success"`
		Data    struct {
			RunnerId    int64  `json:"runnerId"`
			RunnerToken string `json:"runnerToken"`
			Name        string `json:"name"`
		} `json:"data"`
	}

	if err := json.NewDecoder(resp.Body).Decode(&apiResp); err != nil {
		log.Fatalf(colorRed+"[✕] Failed to decode server response: %v"+colorReset, err)
	}

	cfg := &Config{
		MasterURL:    strings.TrimRight(*urlFlag, "/"),
		RunnerID:     apiResp.Data.RunnerId,
		RunnerToken:  apiResp.Data.RunnerToken,
		Name:         apiResp.Data.Name,
		WorkspaceDir: "./workspaces",
	}

	if err := SaveConfig(*configPathFlag, cfg); err != nil {
		log.Fatalf(colorRed+"[✕] Failed to save config: %v"+colorReset, err)
	}

	fmt.Println(colorMuted + "────────────────────────────────────────────────────────" + colorReset)
	fmt.Printf(colorGreen+colorBold+"[✓] Runner '%s' registered successfully! (ID: %d)\n"+colorReset, cfg.Name, cfg.RunnerID)
	fmt.Printf(colorMuted+"    Config saved to: %s\n"+colorReset, *configPathFlag)
	fmt.Printf(colorPurple+colorBold+"    To start execution, run: "+colorReset+"zovik-agent run\n")
	fmt.Println(colorMuted + "────────────────────────────────────────────────────────" + colorReset)
}

func runAgent(args []string) {
	fs := flag.NewFlagSet("run", flag.ExitOnError)
	configPathFlag := fs.String("config", "config.yaml", "Path to config file")
	_ = fs.Parse(args)

	cfg, err := LoadConfig(*configPathFlag)
	if err != nil {
		printBanner()
		log.Fatalf(colorRed+"[✕] Failed to load config file '%s': %v. Did you run 'zovik-agent register' first?"+colorReset, *configPathFlag, err)
	}

	printBanner()
	fmt.Println(colorMuted + "  ────────────────────────────────────────────────────────" + colorReset)
	fmt.Printf("  "+colorPurple+"⚡ Master URL"+colorReset+" : %s\n", cfg.MasterURL)
	fmt.Printf("  "+colorBlue+"🤖 Runner ID "+colorReset+" : %d ("+colorBold+"%s"+colorReset+")\n", cfg.RunnerID, cfg.Name)
	fmt.Printf("  "+colorMuted+"📂 Workspace "+colorReset+" : %s\n", cfg.WorkspaceDir)
	fmt.Printf("  "+colorGreen+"🟢 Status    "+colorReset+" : Connected & Pulling jobs via long-poll\n")
	fmt.Println(colorMuted + "  ────────────────────────────────────────────────────────" + colorReset)

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	// Handle graceful shutdown
	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, syscall.SIGINT, syscall.SIGTERM)
	go func() {
		<-sigChan
		fmt.Println("\nStopping runner agent gracefully...")
		cancel()
	}()

	// 1. Heartbeat loop
	go startHeartbeat(ctx, cfg)

	// 2. Long Polling Job loop
	startJobPoller(ctx, cfg)
}

func startHeartbeat(ctx context.Context, cfg *Config) {
	ticker := time.NewTicker(10 * time.Second)
	defer ticker.Stop()

	endpoint := fmt.Sprintf("%s/api/v1/runners/%d/heartbeat", cfg.MasterURL, cfg.RunnerID)

	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			req, err := http.NewRequestWithContext(ctx, "POST", endpoint, nil)
			if err != nil {
				continue
			}
			req.Header.Set("Authorization", "Bearer "+cfg.RunnerToken)

			resp, err := httpClient.Do(req)
			if err != nil {
				log.Printf("[Heartbeat] Failed: %v", err)
				continue
			}
			resp.Body.Close()
		}
	}
}

func startJobPoller(ctx context.Context, cfg *Config) {
	endpoint := fmt.Sprintf("%s/api/v1/runners/%d/jobs/next", cfg.MasterURL, cfg.RunnerID)

	for {
		select {
		case <-ctx.Done():
			return
		default:
			req, err := http.NewRequestWithContext(ctx, "GET", endpoint, nil)
			if err != nil {
				time.Sleep(2 * time.Second)
				continue
			}
			req.Header.Set("Authorization", "Bearer "+cfg.RunnerToken)

			resp, err := httpClient.Do(req)
			if err != nil {
				log.Printf("[Poller] Connection error: %v. Retrying in 3s...", err)
				time.Sleep(3 * time.Second)
				continue
			}

			if resp.StatusCode == http.StatusNoContent {
				// No jobs available right now, loop immediately for next long-poll
				resp.Body.Close()
				continue
			}

			if resp.StatusCode == http.StatusOK {
				var job JobPayload
				if err := json.NewDecoder(resp.Body).Decode(&job); err != nil {
					log.Printf("[Poller] Error decoding job payload: %v", err)
					resp.Body.Close()
					continue
				}
				resp.Body.Close()

				log.Printf(colorBlue+colorBold+"[◐] RUNNING"+colorReset+" Step '%s' (ID: %d, build: #%d)", job.StepName, job.StepId, job.BuildId)
				processJob(ctx, cfg, &job)
			} else {
				resp.Body.Close()
				time.Sleep(2 * time.Second)
			}
		}
	}
}

func processJob(ctx context.Context, cfg *Config, job *JobPayload) {
	lineCounter := 1
	logEndpoint := fmt.Sprintf("%s/api/v1/runners/%d/jobs/%d/logs", cfg.MasterURL, cfg.RunnerID, job.StepId)

	streamLogs := func(lines []string) {
		fromLine := lineCounter
		lineCounter += len(lines)

		payload := map[string]interface{}{
			"fromLine": fromLine,
			"lines":    lines,
			"stream":   "STDOUT",
		}
		data, _ := json.Marshal(payload)
		req, _ := http.NewRequest("POST", logEndpoint, bytes.NewReader(data))
		req.Header.Set("Authorization", "Bearer "+cfg.RunnerToken)
		req.Header.Set("Content-Type", "application/json")
		if resp, err := httpClient.Do(req); err == nil {
			resp.Body.Close()
		}
	}

	exitCode, status, err := ExecuteDockerJob(ctx, job, cfg.WorkspaceDir, streamLogs)
	if err != nil {
		log.Printf(colorRed+"[✕] Step '%s' execution error: %v"+colorReset, job.StepName, err)
	}

	if status == "SUCCESS" {
		log.Printf(colorGreen+colorBold+"[✓] SUCCESS"+colorReset+" Step '%s' finished with exit code %d", job.StepName, exitCode)
	} else {
		log.Printf(colorRed+colorBold+"[✕] FAILED"+colorReset+" Step '%s' finished with exit code %d (status: %s)", job.StepName, exitCode, status)
	}

	// Report completion
	completeEndpoint := fmt.Sprintf("%s/api/v1/runners/%d/jobs/%d/complete", cfg.MasterURL, cfg.RunnerID, job.StepId)
	completePayload := map[string]interface{}{
		"exitCode": exitCode,
		"status":   status,
	}
	data, _ := json.Marshal(completePayload)
	req, _ := http.NewRequest("POST", completeEndpoint, bytes.NewReader(data))
	req.Header.Set("Authorization", "Bearer "+cfg.RunnerToken)
	req.Header.Set("Content-Type", "application/json")
	if resp, err := httpClient.Do(req); err == nil {
		resp.Body.Close()
	}
}
