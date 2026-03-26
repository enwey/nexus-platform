$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$jdkHome = Join-Path $root ".tools\jdk\jdk-17.0.18+8"
$mavenHome = Join-Path $root ".tools\maven\apache-maven-3.9.9"

if (-not (Test-Path $jdkHome)) {
  throw "JDK not found: $jdkHome"
}

if (-not (Test-Path $mavenHome)) {
  throw "Maven not found: $mavenHome"
}

$env:JAVA_HOME = $jdkHome
$env:MAVEN_HOME = $mavenHome
$env:Path = "$($jdkHome)\bin;$($mavenHome)\bin;$env:Path"

Write-Host "JAVA_HOME=$env:JAVA_HOME"
Write-Host "MAVEN_HOME=$env:MAVEN_HOME"

