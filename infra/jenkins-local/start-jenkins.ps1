$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

$repoRoot = (Resolve-Path (Join-Path $scriptDir "..\..")).Path.Replace("\", "/")
$deployRoot = (Join-Path $scriptDir "deployments").Replace("\", "/")

if (-not (Test-Path $deployRoot)) {
    New-Item -ItemType Directory -Path $deployRoot -Force | Out-Null
}

if (-not (Test-Path .env)) {
@"
JENKINS_ADMIN_ID=admin
JENKINS_ADMIN_PASSWORD=IdolGlow!7077
IDOLGLOW_WORKSPACE=$repoRoot
JENKINS_DEPLOY_HOST_PATH=$deployRoot
GITHUB_CREDENTIALS_ID=github-http-token
GITHUB_USERNAME=
GITHUB_TOKEN=
"@ | Set-Content -Path .env -Encoding UTF8
}

docker compose up -d --build

Write-Host ""
Write-Host "Jenkins 접속 URL : http://localhost:7077" -ForegroundColor Green
Write-Host "관리자 ID        : admin (또는 .env의 JENKINS_ADMIN_ID)" -ForegroundColor Green
Write-Host "비밀번호         : .env 파일의 JENKINS_ADMIN_PASSWORD 참고" -ForegroundColor Green
