package main

import (
	"os"

	"gopkg.in/yaml.v3"
)

type Config struct {
	MasterURL    string `yaml:"master_url"`
	RunnerID     int64  `yaml:"runner_id"`
	RunnerToken  string `yaml:"runner_token"`
	Name         string `yaml:"name"`
	WorkspaceDir string `yaml:"workspace_dir"`
}

func LoadConfig(path string) (*Config, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}
	var cfg Config
	if err := yaml.Unmarshal(data, &cfg); err != nil {
		return nil, err
	}
	if cfg.WorkspaceDir == "" {
		cfg.WorkspaceDir = "./workspaces"
	}
	return &cfg, nil
}

func SaveConfig(path string, cfg *Config) error {
	data, err := yaml.Marshal(cfg)
	if err != nil {
		return err
	}
	return os.WriteFile(path, data, 0600)
}
