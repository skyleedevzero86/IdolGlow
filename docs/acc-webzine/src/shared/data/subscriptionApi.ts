import { getApiBaseUrl } from '../../auth/authConfig';
import { acceptLanguageHeader } from '../../ui/i18n/uiLangStorage';

interface ErrorBody {
  readonly message?: string;
}

export interface SubscriptionRequest {
  readonly email: string;
  readonly agreedToPrivacy: boolean;
  readonly subscribeNewsletters: boolean;
  readonly subscribeIssues: boolean;
}

export interface SubscriptionResponse {
  readonly id: number;
  readonly email: string;
  readonly subscribedTargets: readonly string[];
  readonly subscribedAt: string;
  readonly active: boolean;
}

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
    : '구독 신청을 처리하지 못했습니다.';
}

export async function registerIdolGlowSubscription(
  payload: SubscriptionRequest
): Promise<SubscriptionResponse> {
  const response = await fetch(`${getApiBaseUrl()}/subscriptions`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Accept-Language': acceptLanguageHeader(),
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }

  return (await response.json()) as SubscriptionResponse;
}
