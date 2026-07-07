#!/usr/bin/env bash
set -euo pipefail

DEPLOY_ENV="${1:-dev}"
DEPLOY_ROOT="${DEPLOY_ROOT:-/deployments}"
LATEST_DIR="${DEPLOY_ROOT}/${DEPLOY_ENV}/latest"
BACKEND_DIR="${LATEST_DIR}/backend"

if [[ ! -d "${LATEST_DIR}" ]]; then
  echo "배포 디렉터리가 없습니다: ${LATEST_DIR}" >&2
  exit 1
fi

BACKEND_JAR="$(find "${BACKEND_DIR}" -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' 2>/dev/null | sort | tail -n 1 || true)"
if [[ -z "${BACKEND_JAR}" ]]; then
  echo "배포된 백엔드 JAR이 없습니다: ${BACKEND_DIR}" >&2
  exit 1
fi

if [[ ! -f "${LATEST_DIR}/deploy-info.txt" ]]; then
  echo "deploy-info.txt 가 없습니다: ${LATEST_DIR}" >&2
  exit 1
fi

echo "배포 산출물 검증 완료: ${BACKEND_JAR}"
