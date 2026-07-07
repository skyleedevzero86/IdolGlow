/** API 베이스 URL (GraphQL: `{base}/graphql`, OAuth·쿠키와 동일 오리진이어야 함) */
export function getApiBaseUrl(): string {
  const raw = import.meta.env.VITE_API_BASE_URL;
  if (typeof raw === "string" && raw.trim().length > 0) {
    return raw.replace(/\/$/, "");
  }
  const port = import.meta.env.VITE_API_PORT;
  if (typeof port === "string" && /^\d+$/.test(port.trim())) {
    return `http://localhost:${port.trim()}`;
  }
  // Gradle bootRun 기본 포트(application.yml SERVER_PORT:8080). Docker app 프로필은 9090 → .env 또는 VITE_API_PORT 로 지정.
  return "http://localhost:8080";
}

export function getGraphqlUrl(): string {
  return `${getApiBaseUrl()}/graphql`;
}

export function getProfileSettingsUrl(): string {
  const raw = import.meta.env.VITE_PROFILE_URL;
  if (typeof raw === "string" && raw.trim().length > 0) {
    return raw.trim();
  }
  return "http://localhost:3000/mypage";
}
