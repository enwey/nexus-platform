$ErrorActionPreference = "Stop"

function Test-Admin {
  $current = [Security.Principal.WindowsIdentity]::GetCurrent()
  $principal = New-Object Security.Principal.WindowsPrincipal($current)
  return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

if (-not (Test-Admin)) {
  throw "Please run this script as Administrator."
}

choco install -y docker-desktop --no-progress

Write-Host ""
Write-Host "Docker Desktop installed."
Write-Host "Please reboot Windows if prompted, then open Docker Desktop once."

