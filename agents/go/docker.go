package main

import (
	"bufio"
	"context"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
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
}

func ExecuteDockerJob(ctx context.Context, job *JobPayload, baseDir string, logStreamFunc func(lines []string)) (int, string, error) {
	workspace := filepath.Join(baseDir, fmt.Sprintf("build-%d", job.BuildId))
	if err := os.MkdirAll(workspace, 0755); err != nil {
		return 1, "FAILURE", fmt.Errorf("failed to create workspace: %w", err)
	}

	// Write secret files if provided
	if len(job.SecretFiles) > 0 {
		for relPath, content := range job.SecretFiles {
			fullPath := filepath.Join(workspace, relPath)
			_ = os.MkdirAll(filepath.Dir(fullPath), 0700)
			_ = os.WriteFile(fullPath, []byte(content), 0600)
		}
	}

	// Prepare single command string
	joinedCommands := strings.Join(job.Commands, " && ")
	shScript := fmt.Sprintf("set -e; %s", joinedCommands)

	args := []string{
		"run", "--rm",
		"-v", fmt.Sprintf("%s:/workspace", workspace),
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

	cmd := exec.CommandContext(ctx, "docker", args...)

	stdoutPipe, err := cmd.StdoutPipe()
	if err != nil {
		return 1, "FAILURE", err
	}
	cmd.Stderr = cmd.Stdout

	if err := cmd.Start(); err != nil {
		return 1, "FAILURE", fmt.Errorf("failed to start docker container: %w", err)
	}

	// Stream logs in chunks
	scanner := bufio.NewScanner(stdoutPipe)
	var logBatch []string
	ticker := time.NewTicker(200 * time.Millisecond)
	defer ticker.Stop()

	done := make(chan bool)
	go func() {
		for scanner.Scan() {
			line := scanner.Text()
			logBatch = append(logBatch, line)
			if len(logBatch) >= 10 {
				logStreamFunc(logBatch)
				logBatch = nil
			}
		}
		if len(logBatch) > 0 {
			logStreamFunc(logBatch)
			logBatch = nil
		}
		done <- true
	}()

	<-done
	waitErr := cmd.Wait()

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
