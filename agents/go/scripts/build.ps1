# Cross-platform build script for RabotyagaCI Go Agent
param (
    [string]$Target = "all" # all, windows, linux, darwin
)

$DistDir = Join-Path $PSScriptRoot "..\dist"
if (!(Test-Path $DistDir)) {
    New-Item -ItemType Directory -Path $DistDir | Out-Null
}

function Build-Agent($os, $arch, $outputName) {
    Write-Host "Building for $os/$arch -> $outputName..."
    $env:GOOS = $os
    $env:GOARCH = $arch
    $env:CGO_ENABLED = "0"
    $outputPath = Join-Path $DistDir $outputName
    Push-Location (Join-Path $PSScriptRoot "..")
    go build -ldflags="-s -w" -o $outputPath .
    Pop-Location
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  Success: $outputPath" -ForegroundColor Green
    } else {
        Write-Host "  Failed to build $outputName" -ForegroundColor Red
    }
}

switch ($Target) {
    "windows" {
        Build-Agent "windows" "amd64" "rabotyaga-agent-windows-amd64.exe"
    }
    "linux" {
        Build-Agent "linux" "amd64" "rabotyaga-agent-linux-amd64"
        Build-Agent "linux" "arm64" "rabotyaga-agent-linux-arm64"
    }
    "darwin" {
        Build-Agent "darwin" "amd64" "rabotyaga-agent-darwin-amd64"
        Build-Agent "darwin" "arm64" "rabotyaga-agent-darwin-arm64"
    }
    default {
        Build-Agent "windows" "amd64" "rabotyaga-agent-windows-amd64.exe"
        Build-Agent "linux" "amd64" "rabotyaga-agent-linux-amd64"
        Build-Agent "linux" "arm64" "rabotyaga-agent-linux-arm64"
        Build-Agent "darwin" "amd64" "rabotyaga-agent-darwin-amd64"
        Build-Agent "darwin" "arm64" "rabotyaga-agent-darwin-arm64"
    }
}
