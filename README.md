# RabotyagaCI

Modular monolith CI/CD system built using Spring Boot, Java 21, and Docker.

## Features
- **Project CRUD**: Manage target software repositories.
- **Git integration**: Determinstic sparse updates/fetches using JGit.
- **YAML parser**: Configure builds using `.rabotyaga.yaml` pipeline structure.
- **Docker-in-Docker isolation**: Build execution runs inside resource-restricted containers.
- **Webhooks**: Auto-triggers pipelines on GitHub push events (with signature checks).
- **Log streaming**: Real-time log capture and streaming via Server-Sent Events (SSE).
- **Artifact collection**: Automatical archiving of step results on disk and cleanup mechanisms.

---

## Getting Started

### Prerequisites
- Docker Desktop or Docker engine running.
- Java 21 & Gradle 8+ (for local development).

### Run with Docker Compose
To boot up the complete environment (PostgreSQL + RabotyagaCI App):
```bash
docker compose up --build
