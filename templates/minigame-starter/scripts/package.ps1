param(
  [string]$OutputName = "minigame-starter.zip"
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$releaseDir = Join-Path $projectRoot "release"
$outputPath = Join-Path $releaseDir $OutputName

if (-not (Test-Path $releaseDir)) {
  New-Item -ItemType Directory -Path $releaseDir | Out-Null
}

if (Test-Path $outputPath) {
  Remove-Item -Force $outputPath
}

$include = @(
  "index.html",
  "manifest.json",
  "src",
  "assets",
  "config"
)

$tempDir = Join-Path $releaseDir "__pack_temp__"
if (Test-Path $tempDir) {
  Remove-Item -Recurse -Force $tempDir
}
New-Item -ItemType Directory -Path $tempDir | Out-Null

foreach ($item in $include) {
  $src = Join-Path $projectRoot $item
  $dst = Join-Path $tempDir $item
  if (Test-Path $src) {
    Copy-Item -Recurse -Force $src $dst
  }
}

Compress-Archive -Path (Join-Path $tempDir "*") -DestinationPath $outputPath -CompressionLevel Optimal
Remove-Item -Recurse -Force $tempDir

Write-Output "Packed: $outputPath"

