import { getApiBaseUrl } from '../../auth/authConfig';
import { acceptLanguageHeader } from '../../ui/i18n/uiLangStorage';

export type RecommendationMailPreferredBasis = 'AUTO' | 'POPULAR';
export type RecommendationMailConsentSource = 'MY_SURVEY' | 'MYPAGE';
export type RecommendationMailExpectedBasis = 'PERSONALIZED' | 'POPULAR';

export interface RecommendationMailSetting {
  readonly active: boolean;
  readonly preferredBasis: RecommendationMailPreferredBasis;
  readonly consentSource: RecommendationMailConsentSource | null;
  readonly consentedAt: string | null;
  readonly unsubscribedAt: string | null;
  readonly hasUserSurvey: boolean;
  readonly expectedBasis: RecommendationMailExpectedBasis;
  readonly maskedEmail: string | null;
}

export interface SubscribeRecommendationMailInput {
  readonly preferredBasis: RecommendationMailPreferredBasis;
  readonly consentSource: RecommendationMailConsentSource;
}

export interface RecommendationMailUnsubscribePreview {
  readonly valid: boolean;
  readonly maskedEmail: string | null;
  readonly alreadyUnsubscribed: boolean;
}

export interface RecommendationMailUnsubscribeResult {
  readonly processed: boolean;
  readonly message: string;
}

interface ErrorBody {
  readonly message?: string;
}

const withAuth = (accessToken: string, headers?: HeadersInit): HeadersInit => ({
  Authorization: `Bearer ${accessToken}`,
  'Accept-Language': acceptLanguageHeader(),
  ...headers,
});

const publicHeaders = (headers?: HeadersInit): HeadersInit => ({
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

const optionalString = (value: unknown): string | null =>
  typeof value === 'string' && value.trim().length > 0 ? value : null;

function sanitizeSetting(raw: unknown): RecommendationMailSetting {
  const body = (raw ?? {}) as Record<string, unknown>;
  return {
    active: body.active === true,
    preferredBasis: body.preferredBasis === 'POPULAR' ? 'POPULAR' : 'AUTO',
    consentSource:
      body.consentSource === 'MY_SURVEY' || body.consentSource === 'MYPAGE' ? body.consentSource : null,
    consentedAt: optionalString(body.consentedAt),
    unsubscribedAt: optionalString(body.unsubscribedAt),
    hasUserSurvey: body.hasUserSurvey === true,
    expectedBasis: body.expectedBasis === 'PERSONALIZED' ? 'PERSONALIZED' : 'POPULAR',
    maskedEmail: optionalString(body.maskedEmail),
  };
}

export async function fetchRecommendationMailSetting(accessToken: string): Promise<RecommendationMailSetting> {
  const response = await fetch(`${getApiBaseUrl()}/me/recommendation-mail`, {
    method: 'GET',
    credentials: 'include',
    cache: 'no-store',
    headers: withAuth(accessToken),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return sanitizeSetting(await response.json());
}

export async function subscribeRecommendationMail(
  accessToken: string,
  input: SubscribeRecommendationMailInput,
): Promise<RecommendationMailSetting> {
  const response = await fetch(`${getApiBaseUrl()}/me/recommendation-mail`, {
    method: 'PUT',
    credentials: 'include',
    headers: withAuth(accessToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify({
      agreedToAdvertising: true,
      preferredBasis: input.preferredBasis,
      consentSource: input.consentSource,
    }),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return sanitizeSetting(await response.json());
}

export async function unsubscribeRecommendationMail(accessToken: string): Promise<RecommendationMailSetting> {
  const response = await fetch(`${getApiBaseUrl()}/me/recommendation-mail`, {
    method: 'DELETE',
    credentials: 'include',
    headers: withAuth(accessToken),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return sanitizeSetting(await response.json());
}

export async function previewRecommendationMailUnsubscribe(
  token: string,
): Promise<RecommendationMailUnsubscribePreview> {
  const query = new URLSearchParams();
  query.set('token', token);
  const response = await fetch(`${getApiBaseUrl()}/recommendation-mail/unsubscribe?${query}`, {
    method: 'GET',
    cache: 'no-store',
    headers: publicHeaders(),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  const body = (await response.json()) as Record<string, unknown>;
  return {
    valid: body.valid === true,
    maskedEmail: optionalString(body.maskedEmail),
    alreadyUnsubscribed: body.alreadyUnsubscribed === true,
  };
}

export async function confirmRecommendationMailUnsubscribe(
  token: string,
): Promise<RecommendationMailUnsubscribeResult> {
  const response = await fetch(`${getApiBaseUrl()}/recommendation-mail/unsubscribe`, {
    method: 'POST',
    headers: publicHeaders({ 'Content-Type': 'application/json' }),
    body: JSON.stringify({ token }),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  const body = (await response.json()) as Record<string, unknown>;
  return {
    processed: body.processed === true,
    message: optionalString(body.message) ?? '수신 거부 요청을 처리했습니다.',
  };
}
