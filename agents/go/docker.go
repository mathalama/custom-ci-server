package main

import (
	"bufio"
	"context"
	"fmt"
	"io"
	"log"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"sync"
	"time"
)

type JobPayload struct {
	BuildId              int64             `json:"buildId"`
	StepId               int64             `json:"stepId"`
	StepName             string            `json:"stepName"`
	DockerImage          string            `json:"dockerImage"`
	Commands             []string          `json:"commands"`
	EnvironmentVariables map[string]string `json:"environmentVariables"`
	Privileged           bool              `json:"privileged"`
	DockerSocket         bool              `json:"dockerSocket"`
	SecretFiles          map[string]string `json:"secretFiles"`
	RepoUrl              string            `json:"repoUrl"`
	CommitRef            string            `json:"commitRef"`
	GitToken             string            `json:"gitToken"`
}

func ExecuteDockerJob(ctx context.Context, job *JobPayload, baseDir string, logStreamFunc func(lines []string)) (int, string, error) {
	workspace := filepath.Join(baseDir, fmt.Sprintf("build-%d", job.BuildId))
	if err := os.MkdirAll(workspace, 0755); err != nil {
		return 1, "FAILURE", fmt.Errorf("failed to create workspace: %w", err)
	}

	// 1. Prepare and clone git repository if repoUrl is provided and not yet cloned
	if job.RepoUrl != "" {
		gitDir := filepath.Join(workspace, ".git")
		if _, err := os.Stat(gitDir); os.IsNotExist(err) {
			cloneUrl := job.RepoUrl
			if job.GitToken != "" && strings.HasPrefix(job.RepoUrl, "https://") {
				cloneUrl = fmt.Sprintf("https://x-access-token:%s@%s", job.GitToken, strings.TrimPrefix(job.RepoUrl, "https://"))
			}

			log.Printf("[Worker] Cloning repository %s into %s", job.RepoUrl, workspace)
			logStreamFunc([]string{fmt.Sprintf("==> Cloning repository %s...", job.RepoUrl)})

			cloneArgs := []string{"clone", "--depth", "50"}
			if job.CommitRef != "" {
				cloneArgs = append(cloneArgs, "--branch", job.CommitRef)
			}
			cloneArgs = append(cloneArgs, cloneUrl, workspace)

			cloneCmd := exec.CommandContext(ctx, "git", cloneArgs...)
			if out, cloneErr := cloneCmd.CombinedOutput(); cloneErr != nil {
				log.Printf("[Worker] Branch clone failed (%v): %s. Attempting fallback clone...", cloneErr, string(out))
				_ = os.RemoveAll(workspace)
				_ = os.MkdirAll(workspace, 0755)

				genCloneCmd := exec.CommandContext(ctx, "git", "clone", cloneUrl, workspace)
				if genOut, genErr := genCloneCmd.CombinedOutput(); genErr != nil {
					logStreamFunc([]string{fmt.Sprintf("Git clone error: %s", string(genOut))})
					return 1, "FAILURE", fmt.Errorf("git clone failed: %s", string(genOut))
				}

				if job.CommitRef != "" {
					checkoutCmd := exec.CommandContext(ctx, "git", "-C", workspace, "checkout", job.CommitRef)
					_ = checkoutCmd.Run()
				}
			}
			logStreamFunc([]string{"==> Repository cloned successfully."})
		}
	}

	// 2. Write secret files if provided
	if len(job.SecretFiles) > 0 {
		for relPath, content := range job.SecretFiles {
			fullPath := filepath.Join(workspace, relPath)
			_ = os.MkdirAll(filepath.Dir(fullPath), 0700)
			_ = os.WriteFile(fullPath, []byte(content), 0600)
		}
	}

	// 3. Prepare absolute path for Docker volume mounting
	absWorkspace, err := filepath.Abs(workspace)
	if err != nil {
		absWorkspace = workspace
	}
	dockerWorkspace := filepath.ToSlash(absWorkspace)

	// Prepare single command string
	joinedCommands := strings.Join(job.Commands, " && ")
	shScript := fmt.Sprintf("set -e; %s", joinedCommands)

	args := []string{
		"run", "--rm",
		"-v", fmt.Sprintf("%s:/workspace", dockerWorkspace),
		"-w", "/workspace",
	}

	if job.Privileged {
		args = append(args, "--privileged")
	}

	if job.DockerSocket {
		args = append(args, "-v", "/var/run/docker.sock:/var/run/docker.sock")
	}

	for k, v := range job.EnvironmentVariables {
		args = append(args, "-e", fmt.Sprintf("%s=%s", k, v))
	}

	args = append(args, job.DockerImage, "sh", "-c", shScript)

	log.Printf("[Worker] Executing docker %s", strings.Join(args, " "))

	cmd := exec.CommandContext(ctx, "docker", args...)

	pr, pw := io.Pipe()
	cmd.Stdout = pw
	cmd.Stderr = pw

	// Stream logs in chunks
	scanner := bufio.NewScanner(pr)
	var logBatch []string
	var batchMu sync.Mutex
	ticker := time.NewTicker(200 * time.Millisecond)
	defer ticker.Stop()

	done := make(chan struct{})

	// Periodic flush routine
	go func() {
		for {
			select {
			case <-done:
				return
			case <-ticker.C:
				batchMu.Lock()
				if len(logBatch) > 0 {
					logStreamFunc(logBatch)
					logBatch = nil
				}
				batchMu.Unlock()
			}
		}
	}()

	// Scanner routine reading stdout + stderr
	go func() {
		for scanner.Scan() {
			line := scanner.Text()
			log.Printf("[Container] %s", line)
			batchMu.Lock()
			logBatch = append(logBatch, line)
			if len(logBatch) >= 10 {
				logStreamFunc(logBatch)
				logBatch = nil
			}
			batchMu.Unlock()
		}
		close(done)
	}()

	startErr := cmd.Start()
	if startErr != nil {
		pw.Close()
		return 1, "FAILURE", fmt.Errorf("failed to start docker command: %w", startErr)
	}

	waitErr := cmd.Wait()
	pw.Close()

	// Wait for scanner to drain
	<-done

	// Flush remaining logs
	batchMu.Lock()
	if len(logBatch) > 0 {
		logStreamFunc(logBatch)
		logBatch = nil
	}
	batchMu.Unlock()

	exitCode := 0
	status := "SUCCESS"
	if waitErr != nil {
		if exitErr, ok := waitErr.(*exec.ExitError); ok {
			exitCode = exitErr.ExitCode()
		} else {
			exitCode = 1
		}
		status = "FAILURE"
	}

	return exitCode, status, nil
}
