-- Up Migration: Initial Schema Setup

-- 1. Projects
CREATE TABLE projects (
                          id BIGSERIAL PRIMARY KEY,
                          name VARCHAR(255) NOT NULL,
                          repo_url VARCHAR(500) NOT NULL,
                          git_provider VARCHAR(50) NOT NULL CHECK (git_provider IN ('GITHUB', 'GITLAB', 'GITEA')),
                          default_branch VARCHAR(100) DEFAULT 'main' NOT NULL,
                          webhook_secret VARCHAR(255) NOT NULL,
                          pipeline_config_path VARCHAR(255) DEFAULT '.rabotyaga.yaml' NOT NULL,
                          is_active BOOLEAN DEFAULT TRUE NOT NULL,
                          created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
                          updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 2. Builds
CREATE TABLE builds (
                        id BIGSERIAL PRIMARY KEY,
                        project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                        commit_sha VARCHAR(40) NOT NULL,
                        branch VARCHAR(100) NOT NULL,
                        trigger_type VARCHAR(50) NOT NULL CHECK (trigger_type IN ('WEBHOOK', 'MANUAL', 'SCHEDULE')),
                        status VARCHAR(50) NOT NULL CHECK (status IN ('PENDING', 'RUNNING', 'SUCCESS', 'FAILURE', 'CANCELLED')),
                        started_at TIMESTAMP WITH TIME ZONE,
                        finished_at TIMESTAMP WITH TIME ZONE,
                        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX idx_builds_project_id ON builds(project_id);

-- 3. Build Steps
CREATE TABLE build_steps (
                             id BIGSERIAL PRIMARY KEY,
                             build_id BIGINT NOT NULL REFERENCES builds(id) ON DELETE CASCADE,
                             name VARCHAR(255) NOT NULL,
                             step_order INT NOT NULL,
                             docker_image VARCHAR(500) NOT NULL,
                             commands TEXT NOT NULL,
                             status VARCHAR(50) NOT NULL CHECK (status IN ('PENDING', 'RUNNING', 'SUCCESS', 'FAILURE', 'SKIPPED')),
                             exit_code INT,
                             started_at TIMESTAMP WITH TIME ZONE,
                             finished_at TIMESTAMP WITH TIME ZONE,
                             duration_ms BIGINT
);
CREATE INDEX idx_build_steps_build_id ON build_steps(build_id);

-- 4. Build Logs
CREATE TABLE build_logs (
                            id BIGSERIAL PRIMARY KEY,
                            build_step_id BIGINT NOT NULL REFERENCES build_steps(id) ON DELETE CASCADE,
                            stream VARCHAR(50) NOT NULL CHECK (stream IN ('STDOUT', 'STDERR')),
                            content TEXT NOT NULL,
                            line_number INT NOT NULL,
                            timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX idx_build_logs_step_line ON build_logs(build_step_id, line_number);

-- 5. Build Artifacts
CREATE TABLE build_artifacts (
                                 id BIGSERIAL PRIMARY KEY,
                                 build_id BIGINT NOT NULL REFERENCES builds(id) ON DELETE CASCADE,
                                 file_name VARCHAR(255) NOT NULL,
                                 file_path VARCHAR(500) NOT NULL,
                                 file_size BIGINT NOT NULL,
                                 content_type VARCHAR(100) NOT NULL,
                                 created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX idx_build_artifacts_build_id ON build_artifacts(build_id);
