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

export type RecommendationReasonCode =
  | 'CONCEPT_MATCH'
  | 'BEHAVIOR_TAG'
  | 'PLACE_MATCH'
  | 'IDOL_MATCH'
  | 'SCHEDULE_FIT'
  | 'EXTRA_TAG';

export type RecommendationFallback = 'POPULAR';

export interface RecommendationReason {
  readonly code: RecommendationReasonCode;
  readonly label: string;
}

export interface PersonalizedRecommendationItem {
  readonly id: number;
  readonly name: string;
  readonly description: string;
  readonly basePrice: number;
  readonly optionsTotalPrice: number;
  readonly minPrice: number;
  readonly totalPrice: number;
  readonly tagNames: readonly string[];
  readonly wishCount: number;
  readonly averageRating: number;
  readonly reviewCount: number;
  readonly matchedTags: readonly string[];
  readonly thumbnailUrl: string | null;
  readonly reasons: readonly RecommendationReason[];
  readonly fallback: RecommendationFallback | null;
}

const REASON_CODES: ReadonlySet<string> = new Set<RecommendationReasonCode>([
  'CONCEPT_MATCH',
  'BEHAVIOR_TAG',
  'PLACE_MATCH',
  'IDOL_MATCH',
  'SCHEDULE_FIT',
  'EXTRA_TAG',
]);

const toNumber = (value: unknown): number => {
  const parsed = typeof value === 'string' ? Number(value) : value;
  return typeof parsed === 'number' && Number.isFinite(parsed) ? parsed : 0;
};

const toStringList = (value: unknown): string[] =>
  Array.isArray(value) ? value.filter((item): item is string => typeof item === 'string') : [];

function sanitizeReasons(value: unknown): RecommendationReason[] {
  if (!Array.isArray(value)) return [];
  const reasons: RecommendationReason[] = [];
  for (const item of value) {
    if (!item || typeof item !== 'object') continue;
    const { code, label } = item as { code?: unknown; label?: unknown };
    if (typeof code !== 'string' || !REASON_CODES.has(code)) continue;
    if (typeof label !== 'string' || !label.trim()) continue;
    reasons.push({ code: code as RecommendationReasonCode, label: label.trim() });
  }
  return reasons;
}

export function sanitizePersonalizedRecommendationItem(raw: unknown): PersonalizedRecommendationItem | null {
  if (!raw || typeof raw !== 'object') return null;
  const item = raw as Record<string, unknown>;
  const id = toNumber(item.id);
  if (id <= 0 || typeof item.name !== 'string') return null;
  return {
    id,
    name: item.name,
    description: typeof item.description === 'string' ? item.description : '',
    basePrice: toNumber(item.basePrice),
    optionsTotalPrice: toNumber(item.optionsTotalPrice),
    minPrice: toNumber(item.minPrice),
    totalPrice: toNumber(item.totalPrice),
    tagNames: toStringList(item.tagNames),
    wishCount: toNumber(item.wishCount),
    averageRating: toNumber(item.averageRating),
    reviewCount: toNumber(item.reviewCount),
    matchedTags: toStringList(item.matchedTags),
    thumbnailUrl: safeImageUrl(typeof item.thumbnailUrl === 'string' ? item.thumbnailUrl : null),
    reasons: sanitizeReasons(item.reasons),
    fallback: item.fallback === 'POPULAR' ? 'POPULAR' : null,
  };
}

export function sanitizePersonalizedRecommendationList(value: unknown): PersonalizedRecommendationItem[] {
  if (!Array.isArray(value)) return [];
  return value
    .map(sanitizePersonalizedRecommendationItem)
    .filter((item): item is PersonalizedRecommendationItem => item !== null);
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
  } catch {}
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

export async function fetchPersonalizedRecommendations(
  accessToken: string,
  size: number,
): Promise<readonly PersonalizedRecommendationItem[]> {
  const query = new URLSearchParams();
  query.set('size', String(size));
  const response = await fetch(`${getApiBaseUrl()}/products/recommendations?${query}`, {
    method: 'GET',
    credentials: 'include',
    cache: 'no-store',
    headers: withOptionalAuth(accessToken),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return sanitizePersonalizedRecommendationList(await response.json());
}
