param(
    [string]$FrontendPort = "5173",
    [string]$BackendUrl = $(if ($env:VITE_API_TARGET) { $env:VITE_API_TARGET } else { "http://localhost:8080" })
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$frontendDir = Join-Path $repoRoot "Frontend"

$env:VITE_API_TARGET = $BackendUrl

Set-Location $frontendDir
& npm.cmd run dev -- --host 127.0.0.1 --port $FrontendPort
