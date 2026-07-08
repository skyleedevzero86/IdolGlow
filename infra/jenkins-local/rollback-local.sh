#!/usr/bin/env bash
set -euo pipefail

DEPLOY_ENV="${1:-dev}"
DEPLOY_ROOT="${DEPLOY_ROOT:-/var/jenkins_home/deployments}"

LATEST_DIR="${DEPLOY_ROOT}/${DEPLOY_ENV}/latest"
PREVIOUS_DIR="${DEPLOY_ROOT}/${DEPLOY_ENV}/previous"

if [[ ! -d "${PREVIOUS_DIR}" ]]; then
  echo "롤백할 이전 배포본이 없습니다: ${PREVIOUS_DIR}" >&2
  exit 1
fi

rm -rf "${LATEST_DIR}"
cp -a "${PREVIOUS_DIR}" "${LATEST_DIR}"

echo "이전 배포본으로 롤백했습니다: ${PREVIOUS_DIR} -> ${LATEST_DIR}"
