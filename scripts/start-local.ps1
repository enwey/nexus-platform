$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

& powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'env-local.ps1')

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
Write-Host "[1/4] Starting infrastructure..."
npm.cmd run infra:up

Write-Host ""
Write-Host "[2/4] Starting backend..."
Start-Process powershell -ArgumentList '-ExecutionPolicy', 'Bypass', '-NoExit', '-Command', "& '$PSScriptRoot\env-local.ps1'; mvn -f '$root\backend\pom.xml' spring-boot:run"

Write-Host ""
Write-Host "[3/4] Starting dev portal..."
Start-Process powershell -ArgumentList '-ExecutionPolicy', 'Bypass', '-NoExit', '-Command', "Set-Location '$root'; npm.cmd run dev:portal"

Write-Host ""
Write-Host "[4/4] Starting ops portal..."
Start-Process powershell -ArgumentList '-ExecutionPolicy', 'Bypass', '-NoExit', '-Command', "Set-Location '$root'; npm.cmd run dev:ops"

Write-Host ""
Write-Host "Startup commands launched."
Write-Host "Dev Portal: http://localhost:5173"
Write-Host "Ops Portal: http://localhost:5174"
Write-Host "Backend:    http://localhost:8080/api/v1/actuator/health"
