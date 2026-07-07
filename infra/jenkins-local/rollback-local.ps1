param(
    [string]$DeployEnv = "dev"
)

$ErrorActionPreference = "Stop"

$deployRoot = if ($env:DEPLOY_ROOT) { $env:DEPLOY_ROOT } else { "D:\intel3\IdolGlow\infra\jenkins-local\deployments" }
$latestDir = Join-Path $deployRoot "$DeployEnv\latest"
$previousDir = Join-Path $deployRoot "$DeployEnv\previous"

if (-not (Test-Path $previousDir)) {
    Write-Error "롤백할 이전 배포본이 없습니다: $previousDir"
}

if (Test-Path $latestDir) {
    Remove-Item -LiteralPath $latestDir -Recurse -Force
}

Copy-Item -Path $previousDir -Destination $latestDir -Recurse -Force
Write-Output "이전 배포본으로 롤백했습니다: $previousDir -> $latestDir"
