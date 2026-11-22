import { getApiBaseUrl } from "../../auth/authConfig";
import { refreshExpiredSession } from "../../auth/sessionRefresh";
import { acceptLanguageHeader } from "../../ui/i18n/uiLangStorage";

export class ApiError extends Error {
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

export type OffsetPage<T> = {
  readonly items: readonly T[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
  readonly hasNext: boolean;
};

type ApiRequestOptions = {
  readonly method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  readonly accessToken?: string | null;
  readonly json?: unknown;
  readonly formData?: FormData;
  readonly query?: Record<string, string | number | null | undefined>;
  readonly signal?: AbortSignal;
};

const statusFallbackMessage = (status: number): string => {
  if (status === 401) return "로그인이 필요합니다.";
  if (status === 403) return "권한이 없습니다.";
  if (status === 404) return "요청한 정보를 찾을 수 없습니다.";
  if (status >= 500) return "서버 요청 처리 중 오류가 발생했습니다.";
  return "요청을 처리하지 못했습니다.";
};

async function readErrorMessage(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as { message?: unknown };
    if (typeof body.message === "string" && body.message.trim()) {
      return body.message.trim();
    }
  } catch {
    return statusFallbackMessage(response.status);
  }
  return statusFallbackMessage(response.status);
}

export function buildApiUrl(
  path: string,
  query?: ApiRequestOptions["query"],
): string {
  const params = new URLSearchParams();
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value === null || value === undefined || value === "") continue;
      params.set(key, String(value));
    }
  }
  const search = params.toString();
  return `${getApiBaseUrl()}${path}${search ? `?${search}` : ""}`;
}

export async function apiRequest<T>(
  path: string,
  options: ApiRequestOptions = {},
): Promise<T> {
  try {
    return await sendApiRequest<T>(path, options);
  } catch (error) {
    if (
      !(error instanceof ApiError) ||
      error.status !== 401 ||
      !options.accessToken
    ) {
      throw error;
    }
    const refreshed = await refreshExpiredSession(options.accessToken);
    if (!refreshed || refreshed === options.accessToken) {
      throw new ApiError("로그인이 만료되었어요. 다시 로그인해 주세요.", 401);
    }
    return sendApiRequest<T>(path, { ...options, accessToken: refreshed });
  }
}

async function sendApiRequest<T>(
  path: string,
  options: ApiRequestOptions,
): Promise<T> {
  const headers: Record<string, string> = {
    "Accept-Language": acceptLanguageHeader(),
  };
  if (options.accessToken) {
    headers.Authorization = `Bearer ${options.accessToken}`;
  }
  let body: BodyInit | undefined;
  if (options.formData) {
    body = options.formData;
  } else if (options.json !== undefined) {
    headers["Content-Type"] = "application/json";
    body = JSON.stringify(options.json);
  }

  let response: Response;
  try {
    response = await fetch(buildApiUrl(path, options.query), {
      method: options.method ?? "GET",
      cache: "no-store",
      credentials: "include",
      headers,
      body,
      signal: options.signal,
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") {
      throw error;
    }
    throw new ApiError(
      "서버에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.",
      0,
    );
  }

  if (!response.ok) {
    throw new ApiError(await readErrorMessage(response), response.status);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

export function apiErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof Error && error.message.trim()) {
    return error.message;
  }
  return fallback;
}

export function isAbortError(error: unknown): boolean {
  return error instanceof DOMException && error.name === "AbortError";
}
