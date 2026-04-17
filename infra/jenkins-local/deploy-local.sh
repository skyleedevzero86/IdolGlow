#!/usr/bin/env bash
set -euo pipefail

DEPLOY_ENV="${1:-dev}"
WORKSPACE_DIR="${2:-$PWD}"
RELEASE_NOTES_FILE="${3:-$WORKSPACE_DIR/changelog-release-notes.txt}"
DEPLOY_ROOT="${DEPLOY_ROOT:-/deployments}"
BUILD_ID="${BUILD_ID:-manual-$(date +%Y%m%d%H%M%S)}"

resolve_frontend_dir() {
  if [[ -f "${WORKSPACE_DIR}/frontend/package.json" ]]; then
    echo "${WORKSPACE_DIR}/frontend"
    return
  fi

  if [[ -f "${WORKSPACE_DIR}/docs/acc-webzine/package.json" ]]; then
    echo "${WORKSPACE_DIR}/docs/acc-webzine"
    return
  fi

  echo ""
}

resolve_frontend_build_dir() {
  local frontend_dir="$1"

  if [[ -d "${frontend_dir}/dist" ]]; then
    echo "${frontend_dir}/dist"
    return
  fi

  if [[ -d "${frontend_dir}/.next" ]]; then
    echo "${frontend_dir}/.next"
    return
  fi

  echo ""
}

TARGET_DIR="${DEPLOY_ROOT}/${DEPLOY_ENV}/${BUILD_ID}"
LATEST_DIR="${DEPLOY_ROOT}/${DEPLOY_ENV}/latest"

mkdir -p "${TARGET_DIR}/backend" "${TARGET_DIR}/frontend"

BACKEND_JAR="$(find "${WORKSPACE_DIR}/backend/build/libs" -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | sort | tail -n 1 || true)"
if [[ -n "${BACKEND_JAR}" ]]; then
  cp "${BACKEND_JAR}" "${TARGET_DIR}/backend/"
fi

FRONTEND_DIR="$(resolve_frontend_dir)"
FRONTEND_BUILD_DIR=""
if [[ -n "${FRONTEND_DIR}" ]]; then
  FRONTEND_BUILD_DIR="$(resolve_frontend_build_dir "${FRONTEND_DIR}")"
fi

if [[ -n "${FRONTEND_BUILD_DIR}" ]]; then
  cp -R "${FRONTEND_BUILD_DIR}/." "${TARGET_DIR}/frontend/"
fi

if [[ -f "${WORKSPACE_DIR}/CHANGELOG.md" ]]; then
  cp "${WORKSPACE_DIR}/CHANGELOG.md" "${TARGET_DIR}/CHANGELOG.md"
fi

if [[ -f "${RELEASE_NOTES_FILE}" ]]; then
  cp "${RELEASE_NOTES_FILE}" "${TARGET_DIR}/release-notes.txt"
fi

cat > "${TARGET_DIR}/deploy-info.txt" <<EOF
DEPLOY_ENV=${DEPLOY_ENV}
BUILD_ID=${BUILD_ID}
DEPLOYED_AT=$(date -Iseconds)
WORKSPACE_DIR=${WORKSPACE_DIR}
BACKEND_JAR=$(basename "${BACKEND_JAR:-}")
FRONTEND_DIR=${FRONTEND_DIR}
FRONTEND_BUILD_DIR=${FRONTEND_BUILD_DIR}
EOF

rm -rf "${LATEST_DIR}"
mkdir -p "${LATEST_DIR}"
cp -R "${TARGET_DIR}/." "${LATEST_DIR}/"

echo "배포 산출물을 다음 경로에 복사했습니다: ${TARGET_DIR}"
