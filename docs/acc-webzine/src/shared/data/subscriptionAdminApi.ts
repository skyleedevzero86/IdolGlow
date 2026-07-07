import { getApiBaseUrl } from '../../auth/authConfig';
import { acceptLanguageHeader } from '../../ui/i18n/uiLangStorage';

export type SubscriptionContentType = 'NEWSLETTER' | 'WEBZINE_ISSUE';
export type SubscriptionScheduleFrequency = 'DAILY' | 'WEEKLY';
export type SubscriptionScheduleDayOfWeek =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY';

export interface AdminSubscriptionSubscriber {
  readonly id: number;
  readonly email: string;
  readonly subscribedTargets: readonly string[];
  readonly active: boolean;
  readonly source: string;
  readonly consentedAt: string;
  readonly subscribedAt: string;
}

export interface AdminSubscriptionDispatch {
  readonly id: number;
  readonly contentType: string;
  readonly contentTypeLabel: string;
  readonly contentSlug: string;
  readonly contentTitle: string;
  readonly contentSummary: string | null;
  readonly dispatchChannel: string;
  readonly dispatchChannelLabel: string;
  readonly dispatchStatus: string;
  readonly dispatchStatusLabel: string;
  readonly recipientCount: number;
  readonly dispatchedAt: string;
}

export interface AdminSubscriptionSchedule {
  readonly id: number;
  readonly contentType: SubscriptionContentType;
  readonly contentTypeLabel: string;
  readonly frequencyType: SubscriptionScheduleFrequency;
  readonly frequencyTypeLabel: string;
  readonly dayOfWeek: SubscriptionScheduleDayOfWeek | null;
  readonly dayOfWeekLabel: string | null;
  readonly dispatchTime: string;
  readonly active: boolean;
  readonly nextDispatchAt: string | null;
}

export interface AdminSubscriptionLatestContent {
  readonly contentType: SubscriptionContentType;
  readonly contentTypeLabel: string;
  readonly title: string;
  readonly slug: string;
  readonly summary: string | null;
  readonly publishedAt: string | null;
}

export interface AdminSubscriptionOverviewResponse {
  readonly totalActive: number;
  readonly totalSubscribers: number;
  readonly newsletterSubscriberCount: number;
  readonly issueSubscriberCount: number;
  readonly totalDispatches: number;
  readonly subscribers: readonly AdminSubscriptionSubscriber[];
  readonly subscriberPage: number;
  readonly subscriberSize: number;
  readonly subscriberTotalElements: number;
  readonly subscriberTotalPages: number;
  readonly subscriberHasNext: boolean;
  readonly dispatches: readonly AdminSubscriptionDispatch[];
  readonly dispatchPage: number;
  readonly dispatchSize: number;
  readonly dispatchTotalElements: number;
  readonly dispatchTotalPages: number;
  readonly dispatchHasNext: boolean;
  readonly schedules: readonly AdminSubscriptionSchedule[];
  readonly latestContents: readonly AdminSubscriptionLatestContent[];
}

export interface FetchAdminSubscriptionOverviewParams {
  readonly subscriberPage: number;
  readonly subscriberSize: number;
  readonly dispatchPage: number;
  readonly dispatchSize: number;
}

export interface UpsertAdminSubscriptionSchedulePayload {
  readonly frequencyType: SubscriptionScheduleFrequency;
  readonly dayOfWeek: SubscriptionScheduleDayOfWeek | null;
  readonly dispatchTime: string;
  readonly active: boolean;
}

interface ErrorBody {
  readonly message?: string;
}

const withBaseHeaders = (accessToken: string): HeadersInit => ({
  Authorization: `Bearer ${accessToken}`,
  'Accept-Language': acceptLanguageHeader(),
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
    : '구독 관리 데이터를 불러오지 못했습니다.';
}

export async function fetchAdminSubscriptionOverview(
  accessToken: string,
  params: FetchAdminSubscriptionOverviewParams
): Promise<AdminSubscriptionOverviewResponse> {
  const search = new URLSearchParams({
    subscriberPage: String(params.subscriberPage),
    subscriberSize: String(params.subscriberSize),
    dispatchPage: String(params.dispatchPage),
    dispatchSize: String(params.dispatchSize),
  });

  const response = await fetch(`${getApiBaseUrl()}/admin/subscriptions?${search.toString()}`, {
    method: 'GET',
    credentials: 'include',
    cache: 'no-store',
    headers: withBaseHeaders(accessToken),
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }

  return (await response.json()) as AdminSubscriptionOverviewResponse;
}

export async function upsertAdminSubscriptionSchedule(
  accessToken: string,
  contentType: SubscriptionContentType,
  payload: UpsertAdminSubscriptionSchedulePayload
): Promise<AdminSubscriptionSchedule> {
  const response = await fetch(`${getApiBaseUrl()}/admin/subscriptions/schedules/${contentType}`, {
    method: 'PUT',
    credentials: 'include',
    headers: {
      ...withBaseHeaders(accessToken),
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }

  return (await response.json()) as AdminSubscriptionSchedule;
}
