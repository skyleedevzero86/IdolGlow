export function getApiBaseUrl(): string {
  const raw = import.meta.env.VITE_API_BASE_URL;
  if (typeof raw === "string" && raw.trim().length > 0) {
    return raw.replace(/\/$/, "");
  }
  const port = import.meta.env.VITE_API_PORT;
  if (typeof port === "string" && /^\d+$/.test(port.trim())) {
    return `http://localhost:${port.trim()}`;
  }
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
