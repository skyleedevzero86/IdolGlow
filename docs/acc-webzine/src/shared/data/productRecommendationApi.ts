import { getApiBaseUrl } from '../../auth/authConfig';
import { acceptLanguageHeader } from '../../ui/i18n/uiLangStorage';

export interface ProductRecommendationItem {
  readonly id: number;
  readonly name: string;
  readonly description: string;
  readonly basePrice: number;
  readonly optionsTotalPrice: number;
  readonly minPrice: number;
  readonly totalPrice: number;
  readonly tagNames: readonly string[];
  readonly thumbnailUrl: string | null;
  readonly wishCount?: number;
  readonly averageRating?: number;
  readonly reviewCount?: number;
  readonly tourAttractionPickCount?: number;
}

interface ErrorBody {
  readonly message?: string;
}

function safeImageUrl(url: string | null | undefined): string | null {
  const trimmed = url?.trim();
  if (!trimmed) return null;
  if (trimmed.includes('mock-cloud.example')) return null;
  return trimmed;
}

export function sanitizeProductRecommendationItem(item: ProductRecommendationItem): ProductRecommendationItem {
  return {
    ...item,
    thumbnailUrl: safeImageUrl(item.thumbnailUrl),
  };
}

const withOptionalAuth = (accessToken: string | null, headers?: HeadersInit): HeadersInit => ({
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
  return response.status >= 500 ? '서버 처리 중 오류가 발생했습니다.' : '요청을 처리하지 못했습니다.';
}

export async function fetchLatestInKoreaRecommendations(
  size?: number,
  tag?: string | null,
): Promise<readonly ProductRecommendationItem[]> {
  const query = new URLSearchParams();
  query.set('size', String(size ?? 20));
  if (tag?.trim()) {
    query.set('tag', tag.trim());
  }
  const response = await fetch(`${getApiBaseUrl()}/products/recommendations/latest-in-korea?${query}`, {
    method: 'GET',
    credentials: 'include',
    cache: 'no-store',
    headers: withOptionalAuth(null),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return ((await response.json()) as ProductRecommendationItem[]).map(sanitizeProductRecommendationItem);
}

export async function fetchAdminPickedRecommendations(
  size?: number,
  tag?: string | null,
  queryText?: string | null,
): Promise<readonly ProductRecommendationItem[]> {
  const query = new URLSearchParams();
  query.set('size', String(size ?? 20));
  if (tag?.trim()) {
    query.set('tag', tag.trim());
  }
  if (queryText?.trim()) {
    query.set('query', queryText.trim());
  }
  const response = await fetch(`${getApiBaseUrl()}/products/recommendations/admin-picked?${query}`, {
    method: 'GET',
    credentials: 'include',
    cache: 'no-store',
    headers: withOptionalAuth(null),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return ((await response.json()) as ProductRecommendationItem[]).map(sanitizeProductRecommendationItem);
}

export async function replaceLatestInKoreaRecommendations(
  accessToken: string,
  productIds: readonly number[],
): Promise<{ readonly productCount: number }> {
  const response = await fetch(`${getApiBaseUrl()}/admin/products/recommendations/latest-in-korea`, {
    method: 'POST',
    credentials: 'include',
    headers: withOptionalAuth(accessToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify({ productIds: [...productIds] }),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return (await response.json()) as { readonly productCount: number };
}

export async function updateProductAdminRecommendation(
  accessToken: string,
  productId: number,
  recommended: boolean,
): Promise<{ readonly productId: number; readonly recommended: boolean }> {
  const response = await fetch(`${getApiBaseUrl()}/admin/products/${productId}/recommendation`, {
    method: 'PUT',
    credentials: 'include',
    headers: withOptionalAuth(accessToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify({ recommended }),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return (await response.json()) as { readonly productId: number; readonly recommended: boolean };
}

export async function updateProductRecommendationScore(
  accessToken: string,
  productId: number,
  score: number,
): Promise<{ readonly productId: number; readonly recommendationScore: number }> {
  const response = await fetch(`${getApiBaseUrl()}/admin/products/${productId}/recommendation-score`, {
    method: 'PUT',
    credentials: 'include',
    headers: withOptionalAuth(accessToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify({ score }),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return (await response.json()) as { readonly productId: number; readonly recommendationScore: number };
}
