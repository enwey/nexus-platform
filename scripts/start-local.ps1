$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

& (Join-Path $PSScriptRoot "env-local.ps1")

$dockerOk = $false
try {
  docker --version | Out-Null
  $dockerOk = $true
} catch {
  $dockerOk = $false
}

if (-not $dockerOk) {
  Write-Host ""
  Write-Host "[ERROR] Docker is not installed or not in PATH."
  Write-Host "Please install Docker Desktop (requires administrator)."
  Write-Host "After installing, reopen terminal and run this script again."
  exit 1
}

Write-Host ""
Write-Host "[1/3] Starting infrastructure..."
npm.cmd run infra:up

Write-Host ""
Write-Host "[2/3] Starting backend..."
Start-Process powershell -ArgumentList "-NoExit", "-Command", "& '$PSScriptRoot\env-local.ps1'; mvn -f '$root\backend\pom.xml' spring-boot:run"

Write-Host ""
Write-Host "[3/3] Starting dev portal..."
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$root'; npm.cmd run dev:portal"

Write-Host ""
Write-Host "Startup commands launched."
Write-Host "Portal:  http://localhost:5173"
Write-Host "Backend: http://localhost:8080/actuator/health"

