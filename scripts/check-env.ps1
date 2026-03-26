$ErrorActionPreference = "Continue"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

Write-Host "== Nexus Environment Check =="

Write-Host ""
Write-Host "[Node]"
node -v
npm.cmd -v

Write-Host ""
Write-Host "[Java + Maven from local tools]"
try {
  & (Join-Path $PSScriptRoot "env-local.ps1")
  java -version
  mvn -v
} catch {
  Write-Host $_.Exception.Message
}

Write-Host ""
Write-Host "[Docker]"
try {
  docker --version
  docker compose version
} catch {
  Write-Host "Docker unavailable: $($_.Exception.Message)"
}

