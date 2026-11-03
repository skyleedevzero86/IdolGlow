import react from "@vitejs/plugin-react";
import { defineConfig, loadEnv } from "vite";

declare const process: {
  cwd(): string;
};

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
  "/search-keywords",
  "/api",
  "/graphql",
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

function bypassSpaNavigation(req: {
  method?: string;
  headers: Record<string, string | string[] | undefined>;
}): string | undefined {
  const accept = String(req.headers?.accept ?? "");
  const method = (req.method ?? "GET").toUpperCase();
  if (method === "GET" && accept.includes("text/html")) {
    return "/index.html";
  }
  return undefined;
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const backendTarget = resolveBackendTarget(env);
  const proxy = Object.fromEntries(
    BACKEND_PROXY_PREFIXES.map((prefix) => [
      prefix,
      { target: backendTarget, changeOrigin: true, bypass: bypassSpaNavigation },
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
