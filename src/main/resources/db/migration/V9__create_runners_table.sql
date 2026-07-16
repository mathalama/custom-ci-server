CREATE TABLE runners (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    host VARCHAR(255) NOT NULL,
    port INTEGER NOT NULL DEFAULT 22,
    username VARCHAR(255) NOT NULL,
    ssh_key TEXT,
    password VARCHAR(255),
    status VARCHAR(50) NOT NULL, -- ONLINE, OFFLINE, BUSY, PROVISIONING, FAILED
    token VARCHAR(255) UNIQUE NOT NULL,
    os VARCHAR(50),
    cpu_cores INTEGER,
    memory_bytes BIGINT,
    install_log TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

ALTER TABLE build_steps ADD COLUMN runner_id BIGINT REFERENCES runners(id);
