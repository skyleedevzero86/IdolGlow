#!/usr/bin/env bash
set -euo pipefail

PREFERRED_ROOT="${1:-/var/jenkins_home/deployments}"
FALLBACK_ROOT="/var/jenkins_home/deployments"

is_writable_deploy_root() {
  local root="$1"
  mkdir -p "${root}/dev" "${root}/staging" "${root}/prod" 2>/dev/null || return 1
  [[ -w "${root}" ]]
}

if is_writable_deploy_root "${PREFERRED_ROOT}"; then
  echo "${PREFERRED_ROOT}"
  exit 0
fi

if [[ "${PREFERRED_ROOT}" != "${FALLBACK_ROOT}" ]] && is_writable_deploy_root "${FALLBACK_ROOT}"; then
  echo "DEPLOY_ROOT '${PREFERRED_ROOT}' is not writable; using '${FALLBACK_ROOT}' instead." >&2
  echo "${FALLBACK_ROOT}"
  exit 0
fi

echo "No writable deploy root found. Tried: ${PREFERRED_ROOT}, ${FALLBACK_ROOT}" >&2
echo "Fix: docker exec -u root idolglow-jenkins bash -c 'mkdir -p /var/jenkins_home/deployments && chown -R jenkins:jenkins /var/jenkins_home/deployments'" >&2
exit 1
