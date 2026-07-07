/**
 * API 베이스는 apiUrl() 로만 조립한다. (베이스는 VITE_API_BASE_URL → 백엔드 server.port 와 동일)
 */
import { apiUrl } from "./apiBase";

export async function fetchSiteHomeContent(): Promise<unknown> {
  const res = await fetch(apiUrl("/site-content/home"), {
    method: "GET",
    credentials: "include",
  });
  if (!res.ok) {
    throw new Error(`site-content/home failed: ${res.status}`);
  }
  return res.json();
}
