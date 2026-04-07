$ErrorActionPreference = "Stop"

$jdkHome = 'D:\tools\ai_install\microsoft-jdk-17'
$mavenHome = 'D:\tools\ai_install\apache-maven-3.9.9'
$m2Home = 'D:\tools\ai_install\.m2'

if (-not (Test-Path $jdkHome)) {
  throw "JDK not found: $jdkHome"
}

if (-not (Test-Path $mavenHome)) {
  throw "Maven not found: $mavenHome"
}

if (-not (Test-Path $m2Home)) {
  New-Item -ItemType Directory -Path $m2Home | Out-Null
}

$env:JAVA_HOME = $jdkHome
$env:MAVEN_HOME = $mavenHome
$env:M2_HOME = $mavenHome
$env:MAVEN_OPTS = "-Dmaven.repo.local=$m2Home\repository"
$env:Path = "$($jdkHome)\bin;$($mavenHome)\bin;$env:Path"

$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/nexus_platform"
$env:SPRING_DATASOURCE_USERNAME = "nexus"
$env:SPRING_DATASOURCE_PASSWORD = "nexus_password"
$env:SPRING_REDIS_HOST = "localhost"
$env:SPRING_REDIS_PORT = "6379"
$env:MINIO_ENDPOINT = "http://localhost:9000"
$env:MINIO_ACCESS_KEY = "minioadmin"
$env:MINIO_SECRET_KEY = "minioadmin"
$env:MINIO_BUCKET_NAME = "nexus-games"
$env:NAKAMA_SERVER_KEY = "defaultkeychanged"
$env:NAKAMA_HTTP_KEY = "defaulthttpkeychanged"
$env:NAKAMA_SERVER_URL = "http://localhost:7351"
$env:SECURITY_JWT_SECRET = "nexus-local-jwt-secret-2026-change-before-prod"
$env:PLATFORM_ALLOW_INSECURE_DEFAULTS = "false"
$env:PLATFORM_BOOTSTRAP_ADMIN_ENABLED = "true"
$env:PLATFORM_BOOTSTRAP_ADMIN_USERNAME = "admin"
$env:PLATFORM_BOOTSTRAP_ADMIN_PASSWORD = "NexusLocalAdmin2026!"
$env:PLATFORM_BOOTSTRAP_ADMIN_EMAIL = "admin@nexus.local"

Write-Host "JAVA_HOME=$env:JAVA_HOME"
Write-Host "MAVEN_HOME=$env:MAVEN_HOME"
Write-Host "MAVEN_OPTS=$env:MAVEN_OPTS"
Write-Host "SPRING_DATASOURCE_URL=$env:SPRING_DATASOURCE_URL"
Write-Host "MINIO_ENDPOINT=$env:MINIO_ENDPOINT"
Write-Host "PLATFORM_BOOTSTRAP_ADMIN_ENABLED=$env:PLATFORM_BOOTSTRAP_ADMIN_ENABLED"
Write-Host "PLATFORM_BOOTSTRAP_ADMIN_USERNAME=$env:PLATFORM_BOOTSTRAP_ADMIN_USERNAME"
