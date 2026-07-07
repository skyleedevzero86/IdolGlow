/**
 * 리이슈 등 /auth/* 호출도 동일한 베이스를 쓴다.
 */
import { apiUrl } from "./apiBase";

export async function reissueAccessToken(): Promise<Response> {
  return fetch(apiUrl("/auth/reissue"), {
    method: "POST",
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
    },
  });
}
