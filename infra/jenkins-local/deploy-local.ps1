param(
    [string]$DeployEnv = "dev",
    [string]$WorkspaceDir = (Get-Location).Path,
    [string]$ReleaseNotesFile = (Join-Path (Get-Location).Path "changelog-release-notes.txt")
)

$ErrorActionPreference = "Stop"

$deployRoot = if ($env:DEPLOY_ROOT) { $env:DEPLOY_ROOT } else { "D:\intel3\IdolGlow\infra\jenkins-local\deployments" }
$buildId = if ($env:BUILD_ID) { $env:BUILD_ID } else { "manual-$(Get-Date -Format 'yyyyMMddHHmmss')" }
$targetDir = Join-Path $deployRoot "$DeployEnv\$buildId"
$latestDir = Join-Path $deployRoot "$DeployEnv\latest"

function Resolve-FrontendDir {
    param([string]$RootPath)

    $frontend = Join-Path $RootPath "frontend\package.json"
    if (Test-Path $frontend) {
        return (Split-Path $frontend -Parent)
    }

    $accWebzine = Join-Path $RootPath "docs\acc-webzine\package.json"
    if (Test-Path $accWebzine) {
        return (Split-Path $accWebzine -Parent)
    }

    return $null
}

function Resolve-FrontendBuildDir {
    param([string]$FrontendDir)

    if (-not $FrontendDir) {
        return $null
    }

    $distDir = Join-Path $FrontendDir "dist"
    if (Test-Path $distDir) {
        return $distDir
    }

    $nextDir = Join-Path $FrontendDir ".next"
    if (Test-Path $nextDir) {
        return $nextDir
    }

    return $null
}

New-Item -ItemType Directory -Force -Path (Join-Path $targetDir "backend") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $targetDir "frontend") | Out-Null

$backendLibDir = Join-Path $WorkspaceDir "backend\build\libs"
$backendJar = Get-ChildItem -Path $backendLibDir -Filter *.jar -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notlike "*-plain.jar" } |
    Sort-Object Name |
    Select-Object -Last 1

if ($backendJar) {
    Copy-Item -Path $backendJar.FullName -Destination (Join-Path $targetDir "backend") -Force
}

$frontendDir = Resolve-FrontendDir -RootPath $WorkspaceDir
$frontendBuildDir = Resolve-FrontendBuildDir -FrontendDir $frontendDir
if ($frontendBuildDir) {
    Copy-Item -Path (Join-Path $frontendBuildDir "*") -Destination (Join-Path $targetDir "frontend") -Recurse -Force
}

$changelog = Join-Path $WorkspaceDir "CHANGELOG.md"
if (Test-Path $changelog) {
    Copy-Item -Path $changelog -Destination (Join-Path $targetDir "CHANGELOG.md") -Force
}

if (Test-Path $ReleaseNotesFile) {
    Copy-Item -Path $ReleaseNotesFile -Destination (Join-Path $targetDir "release-notes.txt") -Force
}

@"
DEPLOY_ENV=$DeployEnv
BUILD_ID=$buildId
DEPLOYED_AT=$(Get-Date -Format o)
WORKSPACE_DIR=$WorkspaceDir
BACKEND_JAR=$($backendJar.Name)
FRONTEND_DIR=$frontendDir
FRONTEND_BUILD_DIR=$frontendBuildDir
"@ | Set-Content -Path (Join-Path $targetDir "deploy-info.txt") -Encoding UTF8

if (Test-Path $latestDir) {
    Remove-Item -LiteralPath $latestDir -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $latestDir | Out-Null
Copy-Item -Path (Join-Path $targetDir "*") -Destination $latestDir -Recurse -Force

Write-Output "배포 산출물을 다음 경로에 복사했습니다: $targetDir"
