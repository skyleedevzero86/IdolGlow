/** 관리자 셸 등에서 사용: 브라우저 탭 기준 최대 체류 시간(벽시계). */
export const ADMIN_SESSION_WALL_MS = 2 * 60 * 60 * 1000;

export const ADMIN_SESSION_UNTIL_KEY = "idolglow_admin_session_until";

export function readAdminSessionDeadlineMs(): number | null {
  const raw = sessionStorage.getItem(ADMIN_SESSION_UNTIL_KEY);
  if (raw == null) {
    return null;
  }
  const until = Number(raw);
  return Number.isFinite(until) ? until : null;
}

export function writeAdminSessionDeadlineMs(untilMs: number): void {
  sessionStorage.setItem(ADMIN_SESSION_UNTIL_KEY, String(untilMs));
}

export function clearAdminSessionDeadline(): void {
  sessionStorage.removeItem(ADMIN_SESSION_UNTIL_KEY);
}

export function formatAdminSessionRemaining(deadlineMs: number, nowMs: number): string {
  const sec = Math.max(0, Math.ceil((deadlineMs - nowMs) / 1000));
  if (sec <= 0) {
    return "곧 로그아웃";
  }
  const h = Math.floor(sec / 3600);
  const m = Math.floor((sec % 3600) / 60);
  const s = sec % 60;
  if (h > 0) {
    return `${h}시간 ${m}분 남음`;
  }
  if (m > 0) {
    return `${m}분 ${s}초 남음`;
  }
  return `${s}초 남음`;
}
