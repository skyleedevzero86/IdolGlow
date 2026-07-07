/**
 * SPA에서 API 베이스 URL을 한곳에서만 읽는다.
 * - Vite: .env.development 에 VITE_API_BASE_URL=http://localhost:8080 (Gradle bootRun 기본, Docker app 은 9090)
 * - 값이 비어 있으면 '' → 상대 경로만 쓰므로, 그때는 Vite proxy 로 백엔드로 넘기거나 env 를 채운다.
 * - Network 에 백엔드 포트와 다른 호스트(:포트)만 나오는데 여기를 안 쓴다면, 다른 파일의 axios baseURL·생성 클라이언트·폴백 상수를 검색한다.
 */
export function getApiBaseUrl(): string {
  const raw = import.meta.env.VITE_API_BASE_URL as string | undefined;
  if (raw == null || String(raw).trim() === "") {
    return "";
  }
  return String(raw).replace(/\/+$/, "");
}

export function apiUrl(path: string): string {
  const base = getApiBaseUrl();
  const p = path.startsWith("/") ? path : `/${path}`;
  return `${base}${p}`;
}
