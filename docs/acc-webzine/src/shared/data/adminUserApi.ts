import { getApiBaseUrl } from '../../auth/authConfig';
import { acceptLanguageHeader } from '../../ui/i18n/uiLangStorage';

export type AdminUserRole = 'USER' | 'ADMIN';
export type AdminUserAccountStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'SUSPENDED' | 'WITHDRAWN';

export interface AdminUserSummary {
  readonly id: number;
  readonly email: string;
  readonly nickname: string;
  readonly role: AdminUserRole;
  readonly roleLabel: string;
  readonly accountStatus: AdminUserAccountStatus;
  readonly accountStatusLabel: string;
  readonly loginFailCount: number;
  readonly locked: boolean;
  readonly platformUsername: string | null;
  readonly profileImageUrl: string | null;
  readonly lastLoginAt: string | null;
  readonly oauthLinked: boolean;
  readonly oauthProviders: readonly string[];
}

export interface AdminUserPageResponse {
  readonly users: readonly AdminUserSummary[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
  readonly hasNext: boolean;
  readonly totalUsers: number;
  readonly adminCount: number;
  readonly suspendedCount: number;
  readonly withdrawnCount: number;
}

export interface FetchAdminUserPageParams {
  readonly page: number;
  readonly size: number;
  readonly keyword?: string;
  readonly role?: AdminUserRole | '';
  readonly accountStatus?: AdminUserAccountStatus | '';
}

interface ErrorBody {
  readonly message?: string;
}

const withBaseHeaders = (accessToken?: string | null, headers?: HeadersInit): HeadersInit => ({
  ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
  'Accept-Language': acceptLanguageHeader(),
  ...headers,
});

async function readErrorMessage(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as ErrorBody;
    if (body.message?.trim()) {
      return body.message;
    }
  } catch {
    // ignore parse failure
  }

  return response.status >= 500
    ? '서버 요청 처리 중 오류가 발생했습니다.'
    : '사용자 관리 요청을 처리하지 못했습니다.';
}

async function requestJson<T>(
  input: RequestInfo | URL,
  init: RequestInit,
  retryWithoutBearer = false
): Promise<T> {
  let response = await fetch(input, { ...init, credentials: 'include' });

  if (
    retryWithoutBearer &&
    response.status === 401 &&
    init.headers instanceof Object &&
    'Authorization' in (init.headers as Record<string, string>)
  ) {
    const nextHeaders = { ...(init.headers as Record<string, string>) };
    delete nextHeaders.Authorization;
    response = await fetch(input, {
      ...init,
      headers: nextHeaders,
      credentials: 'include',
    });
  }

  if (!response.ok) {
    throw new Error(`[${response.status}] ${await readErrorMessage(response)}`);
  }

  return (await response.json()) as T;
}

async function requestJsonWithAdminPathFallback<T>(
  adminPath: string,
  init: RequestInit,
  retryWithoutBearer = false
): Promise<T> {
  const baseUrl = getApiBaseUrl();

  try {
    return await requestJson<T>(`${baseUrl}${adminPath}`, init, retryWithoutBearer);
  } catch (error) {
    if (!(error instanceof Error) || !error.message.startsWith('[404]')) {
      throw error;
    }

    const fallbackPath = adminPath.startsWith('/admin/')
      ? `/api${adminPath}`
      : adminPath.replace('/api/admin/', '/admin/');

    return requestJson<T>(`${baseUrl}${fallbackPath}`, init, retryWithoutBearer);
  }
}

export async function fetchAdminUserPage(
  accessToken: string | null,
  params: FetchAdminUserPageParams
): Promise<AdminUserPageResponse> {
  const search = new URLSearchParams({
    page: String(params.page),
    size: String(params.size),
  });

  if (params.keyword?.trim()) {
    search.set('keyword', params.keyword.trim());
  }
  if (params.role) {
    search.set('role', params.role);
  }
  if (params.accountStatus) {
    search.set('accountStatus', params.accountStatus);
  }

  return requestJsonWithAdminPathFallback<AdminUserPageResponse>(`/admin/users?${search.toString()}`, {
    method: 'GET',
    headers: withBaseHeaders(accessToken),
    cache: 'no-store',
  }, true);
}

export async function updateAdminUserRole(
  accessToken: string | null,
  userId: number,
  role: AdminUserRole
): Promise<AdminUserSummary> {
  return requestJsonWithAdminPathFallback<AdminUserSummary>(`/admin/users/${userId}/role`, {
    method: 'PATCH',
    headers: withBaseHeaders(accessToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify({ role }),
  }, true);
}

export async function updateAdminUserStatus(
  accessToken: string | null,
  userId: number,
  accountStatus: AdminUserAccountStatus
): Promise<AdminUserSummary> {
  return requestJsonWithAdminPathFallback<AdminUserSummary>(`/admin/users/${userId}/status`, {
    method: 'PATCH',
    headers: withBaseHeaders(accessToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify({ accountStatus }),
  }, true);
}

export async function unlockAdminUser(
  accessToken: string | null,
  userId: number
): Promise<AdminUserSummary> {
  return requestJsonWithAdminPathFallback<AdminUserSummary>(`/admin/users/${userId}/unlock`, {
    method: 'POST',
    headers: withBaseHeaders(accessToken),
  }, true);
}
