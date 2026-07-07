import { getApiBaseUrl } from '../../auth/authConfig';
import { acceptLanguageHeader } from '../../ui/i18n/uiLangStorage';

type ErrorBody = {
  readonly message?: string;
};

export type AdminAuthVerificationType =
  | ''
  | 'SIGNUP_EMAIL_CHECK'
  | 'ACCOUNT_RECOVERY_INITIATE'
  | 'ACCOUNT_ID_FIND'
  | 'SIGNUP_EMAIL_VERIFICATION_REQUEST'
  | 'SIGNUP_EMAIL_VERIFICATION_CONFIRM'
  | 'SIGNUP_ACCOUNT_CONFIRM_REQUEST'
  | 'SIGNUP_ACCOUNT_CONFIRM_RESULT'
  | 'PASSWORD_TEMP_ISSUED'
  | 'PASSWORD_CHANGED';

export type AdminAuthVerificationLog = {
  readonly id: number;
  readonly verificationType: string;
  readonly email: string | null;
  readonly username: string | null;
  readonly ipAddress: string;
  readonly success: boolean;
  readonly detail: string | null;
  readonly createdAt: string;
};

export type AdminAuthVerificationPage = {
  readonly logs: readonly AdminAuthVerificationLog[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
};

const withBaseHeaders = (accessToken: string, headers?: HeadersInit): HeadersInit => ({
  Authorization: `Bearer ${accessToken}`,
  'Accept-Language': acceptLanguageHeader(),
  ...headers,
});

async function readErrorMessage(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as ErrorBody;
    if (body.message?.trim()) return body.message;
  } catch {
    // ignore parse errors
  }
  return response.status >= 500 ? '서버 요청 처리 중 오류가 발생했습니다.' : '요청을 처리하지 못했습니다.';
}

async function fetchAuthVerificationWithFallback(
  accessToken: string,
  query: URLSearchParams
): Promise<Response> {
  const baseUrl = getApiBaseUrl();
  const primaryUrl = `${baseUrl}/admin/auth-verifications?${query.toString()}`;
  const primary = await fetch(primaryUrl, {
    method: 'GET',
    headers: withBaseHeaders(accessToken),
    credentials: 'include',
    cache: 'no-store',
  });
  if (primary.status !== 404) {
    return primary;
  }
  const fallbackUrl = `${baseUrl}/api/admin/auth-verifications?${query.toString()}`;
  return fetch(fallbackUrl, {
    method: 'GET',
    headers: withBaseHeaders(accessToken),
    credentials: 'include',
    cache: 'no-store',
  });
}

export async function fetchAdminAuthVerificationPage(
  accessToken: string | null,
  params: {
    readonly page: number;
    readonly size: number;
    readonly verificationType: AdminAuthVerificationType;
    readonly keyword: string;
  }
): Promise<AdminAuthVerificationPage> {
  if (!accessToken) {
    throw new Error('관리자 토큰이 없습니다.');
  }
  const query = new URLSearchParams();
  query.set('page', String(Math.max(1, params.page)));
  query.set('size', String(Math.max(1, params.size)));
  if (params.verificationType) query.set('verificationType', params.verificationType);
  if (params.keyword.trim()) query.set('keyword', params.keyword.trim());

  const response = await fetchAuthVerificationWithFallback(accessToken, query);
  if (!response.ok) {
    if (response.status === 404) {
      throw new Error('인증관리 API 경로를 찾을 수 없습니다. 백엔드 라우팅(/admin 또는 /api/admin)을 확인해 주세요.');
    }
    throw new Error(await readErrorMessage(response));
  }
  return (await response.json()) as AdminAuthVerificationPage;
}
