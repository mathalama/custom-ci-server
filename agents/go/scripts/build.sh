#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
DIST_DIR="$ROOT_DIR/dist"

mkdir -p "$DIST_DIR"

build_target() {
    local os=$1
    local arch=$2
    local output=$3
    echo "Building for $os/$arch -> $output..."
    GOOS=$os GOARCH=$arch CGO_ENABLED=0 go build -ldflags="-s -w" -o "$DIST_DIR/$output" "$ROOT_DIR"
    echo "  Success: $DIST_DIR/$output"
}

case "${1:-all}" in
    windows)
        build_target "windows" "amd64" "rabotyaga-agent-windows-amd64.exe"
        ;;
    linux)
        build_target "linux" "amd64" "rabotyaga-agent-linux-amd64"
        build_target "linux" "arm64" "rabotyaga-agent-linux-arm64"
        ;;
    darwin)
        build_target "darwin" "amd64" "rabotyaga-agent-darwin-amd64"
        build_target "darwin" "arm64" "rabotyaga-agent-darwin-arm64"
        ;;
    all)
        build_target "windows" "amd64" "rabotyaga-agent-windows-amd64.exe"
        build_target "linux" "amd64" "rabotyaga-agent-linux-amd64"
        build_target "linux" "arm64" "rabotyaga-agent-linux-arm64"
        build_target "darwin" "amd64" "rabotyaga-agent-darwin-amd64"
        build_target "darwin" "arm64" "rabotyaga-agent-darwin-arm64"
        ;;
    *)
        echo "Usage: $0 [all|windows|linux|darwin]"
        exit 1
        ;;
esac
