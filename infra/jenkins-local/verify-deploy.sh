#!/usr/bin/env bash
set -euo pipefail

DEPLOY_ENV="${1:-dev}"
DEPLOY_ROOT="${DEPLOY_ROOT:-/var/jenkins_home/deployments}"
LATEST_DIR="${DEPLOY_ROOT}/${DEPLOY_ENV}/latest"
BACKEND_DIR="${LATEST_DIR}/backend"
FRONTEND_DIR="${LATEST_DIR}/frontend"

if [[ ! -d "${LATEST_DIR}" ]]; then
  echo "배포 디렉터리가 없습니다: ${LATEST_DIR}" >&2
  exit 1
fi

if [[ ! -f "${LATEST_DIR}/deploy-info.txt" ]]; then
  echo "deploy-info.txt 가 없습니다: ${LATEST_DIR}" >&2
  exit 1
fi

BACKEND_JAR="$(find "${BACKEND_DIR}" -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' 2>/dev/null | sort | tail -n 1 || true)"
FRONTEND_ARTIFACT_COUNT="$(find "${FRONTEND_DIR}" -mindepth 1 -maxdepth 3 -type f 2>/dev/null | wc -l | tr -d ' ')"

if [[ -z "${BACKEND_JAR}" ]]; then
  if [[ "${DEPLOY_ENV}" == "dev" ]]; then
    echo "dev 환경: 백엔드 JAR 없음 — bootJar 미실행 빌드로 간주합니다."
  else
    echo "배포된 백엔드 JAR이 없습니다: ${BACKEND_DIR}" >&2
    exit 1
  fi
fi

if [[ "${FRONTEND_ARTIFACT_COUNT}" == "0" ]]; then
  if [[ "${DEPLOY_ENV}" == "dev" ]]; then
    echo "dev 환경: 프론트 산출물 없음 — 프론트 빌드 미실행으로 간주합니다."
  else
    echo "배포된 프론트 산출물이 없습니다: ${FRONTEND_DIR}" >&2
    exit 1
  fi
fi

if [[ -n "${BACKEND_JAR}" ]]; then
  echo "배포 산출물 검증 완료: backend=${BACKEND_JAR}, frontend_files=${FRONTEND_ARTIFACT_COUNT}"
else
  echo "배포 산출물 검증 완료: frontend_files=${FRONTEND_ARTIFACT_COUNT}"
fi
