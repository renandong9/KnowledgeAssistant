param(
    [string]$Port = $(if ($env:SERVER_PORT) { $env:SERVER_PORT } else { "8080" }),
    [string]$MysqlPassword = $(if ($env:MYSQL_PASSWORD) { $env:MYSQL_PASSWORD } else { "" })
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$backendDir = Join-Path $repoRoot "Backend"
$maven = "D:\develop\IntelliJIDEA2025.2.3\plugins\maven\lib\maven3\bin\mvn.cmd"

$env:SERVER_PORT = $Port
$env:MYSQL_PASSWORD = $MysqlPassword

if (-not $MysqlPassword) {
    Write-Host "MYSQL_PASSWORD is empty. Set it in the environment or pass -MysqlPassword if your database requires one."
}

Set-Location $backendDir
& $maven "-Dmaven.test.skip=true" spring-boot:run
