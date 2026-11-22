import { type OffsetPage, apiRequest } from "./apiClient";

export type SubscriptionContentType =
  | "NEWSLETTER"
  | "IDOLGLOW_ISSUE"
  | "PRODUCT_RECOMMENDATION";

export const ADVERTISING_DISPATCH_HOURS: readonly number[] = Array.from(
  { length: 13 },
  (_, index) => index + 8,
);

export type SubscriptionScheduleFrequency = "DAILY" | "WEEKLY";
export type SubscriptionScheduleDayOfWeek =
  | "MONDAY"
  | "TUESDAY"
  | "WEDNESDAY"
  | "THURSDAY"
  | "FRIDAY"
  | "SATURDAY"
  | "SUNDAY";

export type SubscriberStatusFilter = "ACTIVE" | "INACTIVE";
export type EmailSubscriberAudience = "NEWSLETTER" | "IDOLGLOW_ISSUE";
export type RecommendationBasis = "AUTO" | "POPULAR";
export type SubscriptionDispatchStatus =
  | "RECORDED"
  | "SENDING"
  | "SENT"
  | "PARTIAL_FAILED"
  | "FAILED";
export type DispatchRecipientStatus = "SENT" | "FAILED" | "SKIPPED";
export type MailSendKind =
  | "PRODUCT_RECOMMENDATION"
  | "NEWSLETTER"
  | "IDOLGLOW"
  | "UNSUBSCRIBE_CONFIRM"
  | "CONSENT_REMINDER"
  | "AUTH"
  | "PAYMENT"
  | "OTHER";
export type MailSendStatus = "SENT" | "FAILED" | "SKIPPED";

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
  readonly sentCount: number;
  readonly failedCount: number;
  readonly skippedCount: number;
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
  readonly schedules: readonly AdminSubscriptionSchedule[];
  readonly latestContents: readonly AdminSubscriptionLatestContent[];
}

export interface UpsertAdminSubscriptionSchedulePayload {
  readonly frequencyType: SubscriptionScheduleFrequency;
  readonly dayOfWeek: SubscriptionScheduleDayOfWeek | null;
  readonly dispatchTime: string;
  readonly active: boolean;
}

export interface AdminEmailSubscriber {
  readonly id: number;
  readonly email: string;
  readonly newsletter: boolean;
  readonly idolglowIssue: boolean;
  readonly subscribedTargets: readonly string[];
  readonly subscribedTargetLabels: readonly string[];
  readonly active: boolean;
  readonly statusLabel: string;
  readonly source: string;
  readonly consentedAt: string;
  readonly subscribedAt: string;
}

export interface AdminRecommendationSubscriber {
  readonly id: number;
  readonly userId: number;
  readonly email: string;
  readonly nickname: string;
  readonly active: boolean;
  readonly statusLabel: string;
  readonly preferredBasis: string;
  readonly preferredBasisLabel: string;
  readonly consentSource: string;
  readonly consentSourceLabel: string;
  readonly consentedAt: string;
  readonly unsubscribedAt: string | null;
  readonly lastSentAt: string | null;
  readonly consentRemindedAt: string | null;
}

export interface AdminDispatchRecipient {
  readonly id: number;
  readonly email: string;
  readonly status: string;
  readonly statusLabel: string;
  readonly sentAt: string | null;
  readonly error: string | null;
  readonly skipReason: string | null;
  readonly skipReasonLabel: string | null;
}

export interface AdminDispatchMailContent {
  readonly mailLogId: number;
  readonly subject: string;
  readonly htmlBody: string | null;
  readonly textBody: string | null;
  readonly status: string;
  readonly statusLabel: string;
  readonly sentAt: string | null;
  readonly createdAt: string;
}

export interface AdminDispatchDetail {
  readonly dispatch: AdminSubscriptionDispatch;
  readonly lastAttemptAt: string | null;
  readonly recipients: OffsetPage<AdminDispatchRecipient>;
  readonly mailContent: AdminDispatchMailContent | null;
}

export interface AdminMailLog {
  readonly id: number;
  readonly kind: string;
  readonly kindLabel: string;
  readonly recipientEmail: string;
  readonly subject: string;
  readonly status: string;
  readonly statusLabel: string;
  readonly error: string | null;
  readonly dispatchId: number | null;
  readonly sentAt: string | null;
  readonly createdAt: string;
}

export interface AdminMailLogDetail extends AdminMailLog {
  readonly htmlBody: string | null;
  readonly textBody: string | null;
}

export interface AdminPageQuery {
  readonly page: number;
  readonly size: number;
}

export interface EmailSubscriberQuery extends AdminPageQuery {
  readonly keyword?: string;
  readonly audience?: EmailSubscriberAudience | "";
  readonly status?: SubscriberStatusFilter | "";
}

export interface RecommendationSubscriberQuery extends AdminPageQuery {
  readonly keyword?: string;
  readonly status?: SubscriberStatusFilter | "";
  readonly basis?: RecommendationBasis | "";
}

export interface DispatchQuery extends AdminPageQuery {
  readonly keyword?: string;
  readonly contentType?: SubscriptionContentType | "";
  readonly status?: SubscriptionDispatchStatus | "";
  readonly from?: string;
  readonly to?: string;
}

export interface DispatchRecipientQuery extends AdminPageQuery {
  readonly keyword?: string;
  readonly status?: DispatchRecipientStatus | "";
}

export interface MailLogQuery extends AdminPageQuery {
  readonly keyword?: string;
  readonly kind?: MailSendKind | "";
  readonly status?: MailSendStatus | "";
  readonly from?: string;
  readonly to?: string;
}

const toCount = (value: unknown): number =>
  typeof value === "number" && Number.isFinite(value) && value > 0
    ? Math.floor(value)
    : 0;

const sanitizeDispatch = (
  dispatch: AdminSubscriptionDispatch,
): AdminSubscriptionDispatch => ({
  ...dispatch,
  recipientCount: toCount(dispatch.recipientCount),
  sentCount: toCount(dispatch.sentCount),
  failedCount: toCount(dispatch.failedCount),
  skippedCount: toCount(dispatch.skippedCount),
});

const toQuery = (
  params: object,
): Record<string, string | number | null | undefined> => {
  const query: Record<string, string | number | null | undefined> = {};
  for (const [key, value] of Object.entries(params)) {
    if (typeof value === "string") {
      const trimmed = value.trim();
      if (trimmed) query[key] = trimmed;
    } else if (typeof value === "number") {
      query[key] = value;
    }
  }
  return query;
};

export async function fetchAdminSubscriptionOverview(
  accessToken: string,
  signal?: AbortSignal,
): Promise<AdminSubscriptionOverviewResponse> {
  const response = await apiRequest<AdminSubscriptionOverviewResponse>(
    "/admin/subscriptions",
    {
      accessToken,
      signal,
      query: {
        subscriberPage: 1,
        subscriberSize: 1,
        dispatchPage: 1,
        dispatchSize: 1,
      },
    },
  );
  return {
    totalActive: toCount(response.totalActive),
    totalSubscribers: toCount(response.totalSubscribers),
    newsletterSubscriberCount: toCount(response.newsletterSubscriberCount),
    issueSubscriberCount: toCount(response.issueSubscriberCount),
    totalDispatches: toCount(response.totalDispatches),
    schedules: Array.isArray(response.schedules) ? response.schedules : [],
    latestContents: Array.isArray(response.latestContents)
      ? response.latestContents
      : [],
  };
}

export function upsertAdminSubscriptionSchedule(
  accessToken: string,
  contentType: SubscriptionContentType,
  payload: UpsertAdminSubscriptionSchedulePayload,
): Promise<AdminSubscriptionSchedule> {
  return apiRequest<AdminSubscriptionSchedule>(
    `/admin/subscriptions/schedules/${contentType}`,
    { method: "PUT", accessToken, json: payload },
  );
}

export function fetchAdminEmailSubscribers(
  accessToken: string,
  params: EmailSubscriberQuery,
  signal?: AbortSignal,
): Promise<OffsetPage<AdminEmailSubscriber>> {
  return apiRequest<OffsetPage<AdminEmailSubscriber>>(
    "/admin/subscriptions/email-subscribers",
    { accessToken, signal, query: toQuery(params) },
  );
}

export function fetchAdminRecommendationSubscribers(
  accessToken: string,
  params: RecommendationSubscriberQuery,
  signal?: AbortSignal,
): Promise<OffsetPage<AdminRecommendationSubscriber>> {
  return apiRequest<OffsetPage<AdminRecommendationSubscriber>>(
    "/admin/subscriptions/recommendation-subscribers",
    { accessToken, signal, query: toQuery(params) },
  );
}

export async function fetchAdminSubscriptionDispatches(
  accessToken: string,
  params: DispatchQuery,
  signal?: AbortSignal,
): Promise<OffsetPage<AdminSubscriptionDispatch>> {
  const response = await apiRequest<OffsetPage<AdminSubscriptionDispatch>>(
    "/admin/subscriptions/dispatches",
    { accessToken, signal, query: toQuery(params) },
  );
  return { ...response, items: response.items.map(sanitizeDispatch) };
}

export async function fetchAdminSubscriptionDispatchDetail(
  accessToken: string,
  dispatchId: number,
  params: DispatchRecipientQuery,
  signal?: AbortSignal,
): Promise<AdminDispatchDetail> {
  const response = await apiRequest<AdminDispatchDetail>(
    `/admin/subscriptions/dispatches/${dispatchId}`,
    { accessToken, signal, query: toQuery(params) },
  );
  return { ...response, dispatch: sanitizeDispatch(response.dispatch) };
}

export function fetchAdminMailLogs(
  accessToken: string,
  params: MailLogQuery,
  signal?: AbortSignal,
): Promise<OffsetPage<AdminMailLog>> {
  return apiRequest<OffsetPage<AdminMailLog>>(
    "/admin/subscriptions/mail-logs",
    { accessToken, signal, query: toQuery(params) },
  );
}

export function fetchAdminMailLog(
  accessToken: string,
  mailLogId: number,
  signal?: AbortSignal,
): Promise<AdminMailLogDetail> {
  return apiRequest<AdminMailLogDetail>(
    `/admin/subscriptions/mail-logs/${mailLogId}`,
    { accessToken, signal },
  );
}
