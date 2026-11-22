import { getApiBaseUrl } from "./authConfig";

type SessionRefreshHandler = (staleToken: string) => Promise<string | null>;

let handler: SessionRefreshHandler | null = null;
let inflight: Promise<string | null> | null = null;
let observerInstalled = false;

export function registerSessionRefreshHandler(
  next: SessionRefreshHandler,
): () => void {
  handler = next;
  return () => {
    if (handler === next) handler = null;
  };
}

export function refreshExpiredSession(
  staleToken: string,
): Promise<string | null> {
  if (!handler) return Promise.resolve(null);
  if (inflight) return inflight;
  const run = handler(staleToken).finally(() => {
    inflight = null;
  });
  inflight = run;
  return run;
}

const readBearerToken = (
  input: RequestInfo | URL,
  init?: RequestInit,
): string | null => {
  const headers = new Headers(
    init?.headers ?? (input instanceof Request ? input.headers : undefined),
  );
  const value = headers.get("Authorization");
  if (!value?.startsWith("Bearer ")) return null;
  return value.slice("Bearer ".length).trim() || null;
};

const isBackendRequest = (input: RequestInfo | URL): boolean => {
  const raw =
    typeof input === "string"
      ? input
      : input instanceof URL
        ? input.toString()
        : input.url;
  if (raw.startsWith("/")) return !raw.startsWith("/auth/");
  const base = getApiBaseUrl();
  return raw.startsWith(base) && !raw.startsWith(`${base}/auth/`);
};

export function installUnauthorizedFetchObserver(): void {
  if (observerInstalled || typeof window === "undefined") return;
  observerInstalled = true;
  const originalFetch = window.fetch.bind(window);
  window.fetch = async (input: RequestInfo | URL, init?: RequestInit) => {
    const response = await originalFetch(input, init);
    if (response.status === 401) {
      try {
        const token = readBearerToken(input, init);
        if (token && isBackendRequest(input)) {
          void refreshExpiredSession(token);
        }
      } catch {
        return response;
      }
    }
    return response;
  };
}
