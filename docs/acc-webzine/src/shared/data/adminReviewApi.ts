import { getApiBaseUrl } from '../../auth/authConfig';
import { acceptLanguageHeader } from '../../ui/i18n/uiLangStorage';

export type AdminReviewVisibility = 'ALL' | 'VISIBLE' | 'HIDDEN';

export interface AdminProductReviewImage {
  readonly id: number;
  readonly originalFilename: string;
  readonly url: string;
  readonly sortOrder: number;
}

export interface AdminProductReviewSummary {
  readonly reviewId: number;
  readonly productId: number;
  readonly productName: string;
  readonly userId: number;
  readonly rating: number;
  readonly content: string;
  readonly createdAt: string;
  readonly hidden: boolean;
  readonly hiddenReason: string | null;
  readonly helpfulCount: number;
  readonly images: readonly AdminProductReviewImage[];
}

export interface AdminProductReviewPageResponse {
  readonly reviews: readonly AdminProductReviewSummary[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
  readonly hasNext: boolean;
}

export interface FetchAdminReviewsParams {
  readonly page: number;
  readonly size: number;
  readonly keyword?: string;
  readonly visibility?: AdminReviewVisibility;
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
    // ignore
  }

  return response.status >= 500
    ? '서버 요청 처리 중 오류가 발생했습니다.'
    : '리뷰 관리 요청을 처리하지 못했습니다.';
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

export async function fetchAdminReviews(
  accessToken: string | null,
  params: FetchAdminReviewsParams
): Promise<AdminProductReviewPageResponse> {
  const search = new URLSearchParams({
    page: String(params.page),
    size: String(params.size),
    visibility: params.visibility ?? 'ALL',
  });
  if (params.keyword?.trim()) {
    search.set('keyword', params.keyword.trim());
  }

  return requestJsonWithAdminPathFallback<AdminProductReviewPageResponse>(
    `/admin/reviews?${search.toString()}`,
    {
      method: 'GET',
      headers: withBaseHeaders(accessToken),
      cache: 'no-store',
    },
    true
  );
}

export async function hideAdminReview(
  accessToken: string | null,
  reviewId: number,
  reason?: string | null
): Promise<AdminProductReviewSummary> {
  return requestJsonWithAdminPathFallback<AdminProductReviewSummary>(
    `/admin/reviews/${reviewId}/hide`,
    {
      method: 'PATCH',
      headers: withBaseHeaders(accessToken, { 'Content-Type': 'application/json' }),
      body: JSON.stringify({ reason: reason?.trim() || null }),
    },
    true
  );
}

export async function unhideAdminReview(
  accessToken: string | null,
  reviewId: number
): Promise<AdminProductReviewSummary> {
  return requestJsonWithAdminPathFallback<AdminProductReviewSummary>(
    `/admin/reviews/${reviewId}/unhide`,
    {
      method: 'POST',
      headers: withBaseHeaders(accessToken),
    },
    true
  );
}
