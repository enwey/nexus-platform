$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

Write-Host "[1/5] Load local Java/Maven..."
& (Join-Path $PSScriptRoot "env-local.ps1")

Write-Host "[2/5] Check Docker..."
docker version | Out-Null
docker compose version | Out-Null

Write-Host "[3/5] Start infrastructure..."
npm.cmd run infra:up

Write-Host "[4/5] Start backend..."
Start-Process powershell -ArgumentList "-NoExit", "-Command", "& '$PSScriptRoot\env-local.ps1'; mvn -f '$root\backend\pom.xml' spring-boot:run"

Write-Host "[5/5] Start dev portal..."
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$root'; npm.cmd run dev:portal"

Write-Host ""
Write-Host "Started."
Write-Host "Portal:  http://localhost:5173"
Write-Host "Backend: http://localhost:8080/actuator/health"

