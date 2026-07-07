import react from "@vitejs/plugin-react";
import { defineConfig, loadEnv } from "vite";

declare const process: {
  cwd(): string;
};

/** `/auth/callback` 은 프론트 SPA 라우트 — 백엔드로 프록시하면 OAuth 후 ERR_TOO_MANY_REDIRECTS 발생 */
const BACKEND_AUTH_PROXY_PREFIXES = [
  "/auth/login",
  "/auth/reissue",
  "/auth/logout",
  "/auth/signup",
  "/auth/password",
  "/auth/account",
  "/auth/test",
] as const;

const BACKEND_PROXY_PREFIXES = [
  ...BACKEND_AUTH_PROXY_PREFIXES,
  "/oauth2",
  "/login/oauth2",
  "/site-content",
  "/api",
  "/graphql",
  // SPA 화면(/mypage, /mypage/userInfo)은 Vite history fallback이 처리해야 한다.
  // 그 외 /mypage/* API만 백엔드로 보낸다.
  "^/mypage/(?!userInfo(?:$|[/?]))",
  "/admin",
  "/products",
  "/options",
  "/surveys",
  "/survey-forms",
  "/users",
  "/schedules",
  "/notifications",
  "/payments",
  "/uploads",
  "/health",
  "/platform",
  "/glow-alerts",
  "/ws-mbrd",
] as const;

function resolveBackendTarget(env: Record<string, string>): string {
  const rawBase = env.VITE_API_BASE_URL?.trim();
  if (rawBase) {
    return rawBase.replace(/\/$/, "");
  }
  const port = env.VITE_API_PORT?.trim();
  if (port && /^\d+$/.test(port)) {
    return `http://localhost:${port}`;
  }
  return "http://localhost:8080";
}

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const backendTarget = resolveBackendTarget(env);
  const proxy = Object.fromEntries(
    BACKEND_PROXY_PREFIXES.map((prefix) => [
      prefix,
      { target: backendTarget, changeOrigin: true },
    ]),
  );

  return {
    plugins: [react()],
    server: {
      port: 3000,
      host: true,
      proxy,
    },
  };
});
