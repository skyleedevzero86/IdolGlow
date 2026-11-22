import { buildApiUrl, apiRequest } from "./apiClient";

export type NotificationType =
  | "RESERVATION_CONFIRMED"
  | "RESERVATION_CANCELED"
  | "PAYMENT_FAILED"
  | "PAYMENT_EXPIRED"
  | "RESERVATION_SLOT_AVAILABLE"
  | "RESERVATION_EXPIRING_SOON"
  | "COMMENT_ON_MY_POST"
  | "REPLY_TO_MY_COMMENT"
  | "ADMIN_POST_REPORTED"
  | "ADMIN_POST_AUTO_BLINDED"
  | "ADMIN_PAYMENT_COMPLETED"
  | "ADMIN_PAYMENT_ISSUE";

export type SiteNotification = {
  readonly id: number;
  readonly type: NotificationType | string;
  readonly title: string;
  readonly message: string;
  readonly link: string | null;
  readonly readAt: string | null;
  readonly createdAt: string | null;
};

export const NOTIFICATION_TYPE_LABELS: Record<NotificationType, string> = {
  RESERVATION_CONFIRMED: "예약",
  RESERVATION_CANCELED: "예약",
  PAYMENT_FAILED: "결제",
  PAYMENT_EXPIRED: "결제",
  RESERVATION_SLOT_AVAILABLE: "예약",
  RESERVATION_EXPIRING_SOON: "예약",
  COMMENT_ON_MY_POST: "댓글",
  REPLY_TO_MY_COMMENT: "답글",
  ADMIN_POST_REPORTED: "관리자",
  ADMIN_POST_AUTO_BLINDED: "관리자",
  ADMIN_PAYMENT_COMPLETED: "관리자",
  ADMIN_PAYMENT_ISSUE: "관리자",
};

export const notificationTypeLabel = (type: string): string =>
  NOTIFICATION_TYPE_LABELS[type as NotificationType] ?? "알림";

export const NOTIFICATIONS_CHANGED_EVENT = "site-notifications:changed";

export function emitNotificationsChanged(): void {
  if (typeof window === "undefined") return;
  window.dispatchEvent(new CustomEvent(NOTIFICATIONS_CHANGED_EVENT));
}

export function fetchNotifications(
  accessToken: string,
): Promise<readonly SiteNotification[]> {
  return apiRequest<readonly SiteNotification[]>("/notifications", {
    accessToken,
  });
}

export async function fetchNotificationUnreadCount(
  accessToken: string,
): Promise<number> {
  const body = await apiRequest<{ count?: number }>(
    "/notifications/unread-count",
    { accessToken },
  );
  return Number.isFinite(body?.count) ? Number(body.count) : 0;
}

export async function markNotificationRead(
  notificationId: number,
  accessToken: string,
): Promise<SiteNotification> {
  const result = await apiRequest<SiteNotification>(
    `/notifications/${notificationId}/read`,
    { method: "POST", accessToken },
  );
  emitNotificationsChanged();
  return result;
}

export async function markAllNotificationsRead(
  accessToken: string,
): Promise<void> {
  await apiRequest<void>("/notifications/read-all", {
    method: "POST",
    accessToken,
  });
  emitNotificationsChanged();
}

export function resolveNotificationLink(
  link: string | null | undefined,
): { readonly internal: string } | { readonly external: string } | null {
  const raw = link?.trim();
  if (!raw) return null;
  if (raw.startsWith("/") && !raw.startsWith("//")) {
    return { internal: raw };
  }
  try {
    const url = new URL(raw);
    if (url.protocol !== "http:" && url.protocol !== "https:") return null;
    if (url.origin === window.location.origin) {
      return { internal: `${url.pathname}${url.search}${url.hash}` };
    }
    return { external: url.toString() };
  } catch {
    return null;
  }
}

export function subscribeNotificationStream(
  accessToken: string,
  onNotification: () => void,
): () => void {
  const controller = new AbortController();
  let retryTimer: number | null = null;
  let stopped = false;

  const scheduleRetry = () => {
    if (stopped) return;
    retryTimer = window.setTimeout(connect, 30_000);
  };

  const connect = async () => {
    try {
      const response = await fetch(buildApiUrl("/notifications/stream"), {
        headers: {
          Authorization: `Bearer ${accessToken}`,
          Accept: "text/event-stream",
        },
        credentials: "include",
        cache: "no-store",
        signal: controller.signal,
      });
      if (response.status === 401 || response.status === 403) return;
      if (!response.ok || !response.body) {
        scheduleRetry();
        return;
      }
      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = "";
      while (!stopped) {
        const { done, value } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });
        const blocks = buffer.split(/\r?\n\r?\n/);
        buffer = blocks.pop() ?? "";
        for (const block of blocks) {
          if (/^event:\s*notification\s*$/m.test(block)) {
            onNotification();
          }
        }
      }
      scheduleRetry();
    } catch {
      if (!controller.signal.aborted) scheduleRetry();
    }
  };

  void connect();

  return () => {
    stopped = true;
    controller.abort();
    if (retryTimer !== null) window.clearTimeout(retryTimer);
  };
}
