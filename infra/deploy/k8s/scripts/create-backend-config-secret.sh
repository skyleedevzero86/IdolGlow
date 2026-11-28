#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${NAMESPACE:-idolglow}"
SECRET_NAME="${SECRET_NAME:-idolglow-backend-config}"
CONFIG_DIR="${CONFIG_DIR:-backend/src/main/resources}"

APP_YML="${APP_YML:-${CONFIG_DIR}/application.yml}"
PROFILE_YML="${PROFILE_YML:-${CONFIG_DIR}/application-prod.yml}"
SELECT_YML="${SELECT_YML:-${CONFIG_DIR}/application-select.yml}"
PROFILE_KEY="${PROFILE_KEY:-application-prod.yml}"

args=()
for pair in "application.yml=${APP_YML}" "${PROFILE_KEY}=${PROFILE_YML}" "application-select.yml=${SELECT_YML}"; do
  key="${pair%%=*}"
  file="${pair#*=}"
  if [[ -f "${file}" ]]; then
    args+=("--from-file=${key}=${file}")
  else
    echo "건너뜀: ${file} 가 없습니다" >&2
  fi
done

if [[ ${#args[@]} -eq 0 ]]; then
  echo "설정 파일이 하나도 없습니다. CONFIG_DIR 또는 파일 경로를 확인하세요." >&2
  exit 1
fi

kubectl get namespace "${NAMESPACE}" >/dev/null 2>&1 || kubectl create namespace "${NAMESPACE}"

kubectl -n "${NAMESPACE}" create secret generic "${SECRET_NAME}" "${args[@]}" \
  --dry-run=client -o yaml | kubectl apply -f -

echo "Secret ${NAMESPACE}/${SECRET_NAME} 적용 완료 (키 ${#args[@]}개)"

OAUTH_SECRET_NAME="${OAUTH_SECRET_NAME:-idolglow-backend-oauth}"
oauth_args=()
for key in KAKAO_CLIENT_ID KAKAO_CLIENT_SECRET NAVER_CLIENT_ID NAVER_CLIENT_SECRET; do
  if [[ -n "${!key:-}" ]]; then
    oauth_args+=("--from-literal=${key}=${!key}")
  fi
done

if [[ ${#oauth_args[@]} -eq 0 ]]; then
  echo "건너뜀: 카카오·네이버 키 환경 변수가 없어 ${OAUTH_SECRET_NAME} 를 만들지 않습니다" >&2
else
  kubectl -n "${NAMESPACE}" create secret generic "${OAUTH_SECRET_NAME}" "${oauth_args[@]}"     --dry-run=client -o yaml | kubectl apply -f -
  echo "Secret ${NAMESPACE}/${OAUTH_SECRET_NAME} 적용 완료 (키 ${#oauth_args[@]}개)"
fi

