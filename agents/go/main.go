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

func printUsage() {
	fmt.Println("RabotyagaCI Runner Agent")
	fmt.Println("Usage:")
	fmt.Println("  rabotyaga-agent register --url <server_url> --token <registration_token> [--name <name>]")
	fmt.Println("  rabotyaga-agent run [--config config.yaml]")
}

func runRegister(args []string) {
	fs := flag.NewFlagSet("register", flag.ExitOnError)
	urlFlag := fs.String("url", "http://localhost:8080", "RabotyagaCI Master server URL")
	tokenFlag := fs.String("token", "", "One-time registration token (rb_reg_...)")
	nameFlag := fs.String("name", "", "Runner display name")
	configPathFlag := fs.String("config", "config.yaml", "Path to config file")

	_ = fs.Parse(args)

	if *tokenFlag == "" {
		log.Fatalf("Error: --token is required. Generate one in RabotyagaCI server first.")
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
		log.Fatalf("Failed to connect to RabotyagaCI master: %v", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(resp.Body)
		log.Fatalf("Registration failed (HTTP %d): %s", resp.StatusCode, string(body))
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
		log.Fatalf("Failed to decode server response: %v", err)
	}

	cfg := &Config{
		MasterURL:    strings.TrimRight(*urlFlag, "/"),
		RunnerID:     apiResp.Data.RunnerId,
		RunnerToken:  apiResp.Data.RunnerToken,
		Name:         apiResp.Data.Name,
		WorkspaceDir: "./workspaces",
	}

	if err := SaveConfig(*configPathFlag, cfg); err != nil {
		log.Fatalf("Failed to save config: %v", err)
	}

	fmt.Println("==================================================")
	fmt.Printf("Runner '%s' registered successfully! (ID: %d)\n", cfg.Name, cfg.RunnerID)
	fmt.Printf("Config saved to: %s\n", *configPathFlag)
	fmt.Println("To start the runner, run: rabotyaga-agent run")
	fmt.Println("==================================================")
}

func runAgent(args []string) {
	fs := flag.NewFlagSet("run", flag.ExitOnError)
	configPathFlag := fs.String("config", "config.yaml", "Path to config file")
	_ = fs.Parse(args)

	cfg, err := LoadConfig(*configPathFlag)
	if err != nil {
		log.Fatalf("Failed to load config file '%s': %v. Did you run 'rabotyaga-agent register' first?", *configPathFlag, err)
	}

	fmt.Println("==================================================")
	fmt.Println(" RabotyagaCI Pull-Model Runner Agent starting...")
	fmt.Printf(" Master URL : %s\n", cfg.MasterURL)
	fmt.Printf(" Runner ID  : %d (%s)\n", cfg.RunnerID, cfg.Name)
	fmt.Printf(" Workspace  : %s\n", cfg.WorkspaceDir)
	fmt.Println("==================================================")

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

				log.Printf("[Worker] Received job: step '%s' (ID: %d, build: %d)", job.StepName, job.StepId, job.BuildId)
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
		log.Printf("[Worker] Step '%s' execution error: %v", job.StepName, err)
	}

	log.Printf("[Worker] Step '%s' finished with exit code %d (status: %s)", job.StepName, exitCode, status)

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
