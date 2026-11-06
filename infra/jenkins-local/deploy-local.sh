
set -euo pipefail

DEPLOY_ENV="${1:-dev}"
WORKSPACE_DIR="${2:-$PWD}"
RELEASE_NOTES_FILE="${3:-$WORKSPACE_DIR/changelog-release-notes.txt}"
DEPLOY_ROOT="${DEPLOY_ROOT:-/var/jenkins_home/deployments}"
BUILD_ID="${BUILD_ID:-manual-$(date +%Y%m%d%H%M%S)}"

if [[ -z "${WORKSPACE_DIR}" || "${WORKSPACE_DIR}" == "null" ]]; then
  WORKSPACE_DIR="$(pwd)"
fi

if [[ -z "${RELEASE_NOTES_FILE}" || "${RELEASE_NOTES_FILE}" == "null" ]]; then
  RELEASE_NOTES_FILE="${WORKSPACE_DIR}/changelog-release-notes.txt"
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if ! DEPLOY_ROOT="$("${SCRIPT_DIR}/ensure-deploy-root.sh" "${DEPLOY_ROOT}")"; then
  exit 1
fi
export DEPLOY_ROOT

resolve_frontend_dir() {
  if [[ -f "${WORKSPACE_DIR}/frontend/package.json" ]]; then
    echo "${WORKSPACE_DIR}/frontend"
    return
  fi

  echo ""
}

resolve_frontend_build_dir() {
  local frontend_dir="$1"

  if [[ -d "${frontend_dir}/.next/standalone" ]]; then
    echo "${frontend_dir}/.next/standalone"
    return
  fi

  if [[ -d "${frontend_dir}/out" ]]; then
    echo "${frontend_dir}/out"
    return
  fi

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
PREVIOUS_DIR="${DEPLOY_ROOT}/${DEPLOY_ENV}/previous"

mkdir -p "${TARGET_DIR}/backend" "${TARGET_DIR}/frontend"

if [[ -d "${LATEST_DIR}" ]]; then
  rm -rf "${PREVIOUS_DIR}"
  cp -a "${LATEST_DIR}" "${PREVIOUS_DIR}"
  echo "이전 배포본을 보관했습니다: ${PREVIOUS_DIR}"
fi

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

if [[ "${DEPLOY_ENV}" == "prod" ]]; then
  DEFAULT_LLM_LOCAL_ENABLED="false"
else
  DEFAULT_LLM_LOCAL_ENABLED="true"
fi
OLLAMA_BASE="${OLLAMA_URL:-http://host.docker.internal:11434}"
LLM_LOCAL_ENABLED="${LLM_LOCAL_ENABLED:-${DEFAULT_LLM_LOCAL_ENABLED}}"
LLM_LOCAL_BASE_URL="${LLM_LOCAL_BASE_URL:-${OLLAMA_BASE%/}/v1}"
LLM_LOCAL_MODEL="${LLM_LOCAL_MODEL:-${OLLAMA_MODEL:-qwen2.5:7b}}"
LLM_LOCAL_TIMEOUT_SECONDS="${LLM_LOCAL_TIMEOUT_SECONDS:-60}"
LLM_EXTERNAL_ENABLED="${LLM_EXTERNAL_ENABLED:-false}"
LLM_EXTERNAL_DAILY_LIMIT="${LLM_EXTERNAL_DAILY_LIMIT:-200}"

cat > "${TARGET_DIR}/backend/llm.env" <<EOF
APP_LLM_LOCAL_ENABLED=${LLM_LOCAL_ENABLED}
APP_LLM_LOCAL_BASEURL=${LLM_LOCAL_BASE_URL}
APP_LLM_LOCAL_MODEL=${LLM_LOCAL_MODEL}
APP_LLM_LOCAL_TIMEOUTSECONDS=${LLM_LOCAL_TIMEOUT_SECONDS}
APP_LLM_GATEWAY_EXTERNALENABLED=${LLM_EXTERNAL_ENABLED}
APP_LLM_GATEWAY_EXTERNALDAILYLIMIT=${LLM_EXTERNAL_DAILY_LIMIT}
EOF
echo "LLM 설정: 로컬=${LLM_LOCAL_ENABLED} (${LLM_LOCAL_MODEL}), 외부=${LLM_EXTERNAL_ENABLED}"

cat > "${TARGET_DIR}/deploy-info.txt" <<EOF
DEPLOY_ENV=${DEPLOY_ENV}
BUILD_ID=${BUILD_ID}
DEPLOYED_AT=$(date -Iseconds)
WORKSPACE_DIR=${WORKSPACE_DIR}
BACKEND_JAR=$(basename "${BACKEND_JAR:-}")
FRONTEND_DIR=${FRONTEND_DIR}
FRONTEND_BUILD_DIR=${FRONTEND_BUILD_DIR}
PREVIOUS_DIR=${PREVIOUS_DIR}
LLM_LOCAL_ENABLED=${LLM_LOCAL_ENABLED}
LLM_LOCAL_MODEL=${LLM_LOCAL_MODEL}
LLM_EXTERNAL_ENABLED=${LLM_EXTERNAL_ENABLED}
EOF

rm -rf "${LATEST_DIR}"
mkdir -p "${LATEST_DIR}"
cp -R "${TARGET_DIR}/." "${LATEST_DIR}/"

echo "배포 산출물을 다음 경로에 복사했습니다: ${TARGET_DIR}"
