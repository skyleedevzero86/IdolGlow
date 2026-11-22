import { useCallback, useEffect, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../../../auth/AuthContext";
import {
  GLOW_ALERTS_CHANGED_EVENT,
  type GlowAlertItem,
  fetchGlowAlertUnreadCount,
  fetchGlowAlerts,
  markGlowAlertRead,
} from "../../../shared/data/glowAlertsApi";
import {
  NOTIFICATIONS_CHANGED_EVENT,
  type SiteNotification,
  fetchNotificationUnreadCount,
  fetchNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  notificationTypeLabel,
  resolveNotificationLink,
  subscribeNotificationStream,
} from "../../../shared/data/notificationApi";
import styles from "./NotificationBell.module.css";

const POLL_INTERVAL_MS = 60_000;
const MAX_ITEMS = 20;

type BellItem = {
  readonly key: string;
  readonly id: number;
  readonly source: "notification" | "glow";
  readonly label: string;
  readonly title: string;
  readonly message: string;
  readonly link: string | null;
  readonly createdAt: string | null;
  readonly unread: boolean;
};

const BellIcon = () => (
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    aria-hidden="true"
  >
    <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" />
    <path d="M13.73 21a2 2 0 0 1-3.46 0" />
  </svg>
);

const toTime = (value: string | null): number => {
  if (!value) return 0;
  const parsed = Date.parse(value);
  return Number.isNaN(parsed) ? 0 : parsed;
};

const relativeLabel = (value: string | null): string => {
  const time = toTime(value);
  if (!time) return "";
  const minutes = Math.max(0, Math.floor((Date.now() - time) / 60_000));
  if (minutes < 1) return "방금 전";
  if (minutes < 60) return `${minutes}분 전`;
  if (minutes < 60 * 24) return `${Math.floor(minutes / 60)}시간 전`;
  if (minutes < 60 * 24 * 7) return `${Math.floor(minutes / (60 * 24))}일 전`;
  return `${Math.max(1, Math.floor(minutes / (60 * 24 * 7)))}주 전`;
};

const fromNotification = (item: SiteNotification): BellItem => ({
  key: `notification-${item.id}`,
  id: item.id,
  source: "notification",
  label: notificationTypeLabel(item.type),
  title: item.title,
  message: item.message,
  link: item.link,
  createdAt: item.createdAt,
  unread: !item.readAt,
});

const fromGlowAlert = (item: GlowAlertItem): BellItem => ({
  key: `glow-${item.id}`,
  id: item.id,
  source: "glow",
  label: item.channelLabel || item.categoryLabel,
  title: item.senderName,
  message: item.message,
  link: null,
  createdAt: item.receivedAt,
  unread: item.unread,
});

const mergeItems = (
  notifications: readonly SiteNotification[],
  glowAlerts: readonly GlowAlertItem[],
): BellItem[] => {
  const notificationIds = new Set(notifications.map((item) => item.id));
  return [
    ...notifications.map(fromNotification),
    ...glowAlerts
      .filter((item) => !notificationIds.has(item.id))
      .map(fromGlowAlert),
  ]
    .sort((a, b) => toTime(b.createdAt) - toTime(a.createdAt))
    .slice(0, MAX_ITEMS);
};

type NotificationBellProps = {
  readonly buttonClassName?: string;
  readonly badgeClassName?: string;
};

export function NotificationBell({
  buttonClassName,
  badgeClassName,
}: NotificationBellProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const { accessToken, authReady, user } = useAuth();
  const [open, setOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const [items, setItems] = useState<readonly BellItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const wrapRef = useRef<HTMLDivElement>(null);
  const openRef = useRef(open);
  openRef.current = open;
  const memberToken = authReady && user && accessToken ? accessToken : null;

  const loadCount = useCallback(async () => {
    if (!authReady) return;
    try {
      const count = memberToken
        ? await fetchNotificationUnreadCount(memberToken)
        : await fetchGlowAlertUnreadCount(null);
      setUnreadCount(count);
    } catch {
      return;
    }
  }, [authReady, memberToken]);

  const loadItems = useCallback(async () => {
    if (!authReady) return;
    setLoading(true);
    setError(null);
    try {
      if (memberToken) {
        const [notifications, glowPage] = await Promise.all([
          fetchNotifications(memberToken),
          fetchGlowAlerts({
            page: 1,
            size: MAX_ITEMS,
            status: "unread",
            category: "all",
            accessToken: memberToken,
          }).catch(() => null),
        ]);
        setItems(mergeItems(notifications, glowPage?.items ?? []));
      } else {
        const glowPage = await fetchGlowAlerts({
          page: 1,
          size: MAX_ITEMS,
          status: "unread",
          category: "all",
          accessToken: null,
        });
        setItems(mergeItems([], glowPage.items));
      }
    } catch (loadError) {
      setItems([]);
      setError(
        loadError instanceof Error
          ? loadError.message
          : "알림을 불러오지 못했습니다.",
      );
    } finally {
      setLoading(false);
    }
  }, [authReady, memberToken]);

  useEffect(() => {
    if (!authReady) return;
    if (!memberToken) {
      setItems([]);
      void loadCount();
      return;
    }
    void loadCount();
    const timer = window.setInterval(() => void loadCount(), POLL_INTERVAL_MS);
    return () => window.clearInterval(timer);
  }, [authReady, loadCount, memberToken]);

  useEffect(() => {
    if (!memberToken) return;
    let debounce: number | null = null;
    const unsubscribe = subscribeNotificationStream(memberToken, () => {
      if (debounce !== null) window.clearTimeout(debounce);
      debounce = window.setTimeout(() => {
        void loadCount();
        if (openRef.current) void loadItems();
      }, 600);
    });
    return () => {
      unsubscribe();
      if (debounce !== null) window.clearTimeout(debounce);
    };
  }, [loadCount, loadItems, memberToken]);

  useEffect(() => {
    const refresh = () => void loadCount();
    window.addEventListener(NOTIFICATIONS_CHANGED_EVENT, refresh);
    window.addEventListener(GLOW_ALERTS_CHANGED_EVENT, refresh);
    return () => {
      window.removeEventListener(NOTIFICATIONS_CHANGED_EVENT, refresh);
      window.removeEventListener(GLOW_ALERTS_CHANGED_EVENT, refresh);
    };
  }, [loadCount]);

  useEffect(() => {
    if (open) void loadItems();
  }, [loadItems, open]);

  // biome-ignore lint/correctness/useExhaustiveDependencies: route change is an intentional re-run trigger
  useEffect(() => {
    setOpen(false);
  }, [location.pathname, location.search, location.hash]);

  useEffect(() => {
    if (!open) return;
    const onMouseDown = (event: MouseEvent) => {
      if (!wrapRef.current?.contains(event.target as Node)) setOpen(false);
    };
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setOpen(false);
    };
    document.addEventListener("mousedown", onMouseDown);
    window.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("mousedown", onMouseDown);
      window.removeEventListener("keydown", onKeyDown);
    };
  }, [open]);

  const openItem = async (item: BellItem) => {
    if (item.unread) {
      setItems((current) =>
        current.map((entry) =>
          entry.key === item.key ? { ...entry, unread: false } : entry,
        ),
      );
      setUnreadCount((count) => Math.max(0, count - 1));
      try {
        if (item.source === "notification" && memberToken) {
          await markNotificationRead(item.id, memberToken);
        } else {
          await markGlowAlertRead(item.id, memberToken);
        }
      } catch {
        void loadCount();
      }
    }
    setOpen(false);
    const target = resolveNotificationLink(item.link);
    if (!target) {
      navigate("/glow-alerts");
      return;
    }
    if ("internal" in target) {
      navigate(target.internal);
      return;
    }
    window.location.assign(target.external);
  };

  const readAll = async () => {
    if (!memberToken) return;
    try {
      await markAllNotificationsRead(memberToken);
      setItems((current) =>
        current.map((item) => ({ ...item, unread: false })),
      );
      setUnreadCount(0);
    } catch (readError) {
      setError(
        readError instanceof Error
          ? readError.message
          : "알림을 읽음 처리하지 못했습니다.",
      );
    }
  };

  const badge = unreadCount > 99 ? "99+" : String(unreadCount);

  return (
    <div className={styles.wrap} ref={wrapRef}>
      <button
        type="button"
        className={buttonClassName}
        aria-label={
          unreadCount > 0
            ? `알림 열기, 읽지 않은 알림 ${unreadCount}개`
            : "알림 열기"
        }
        aria-haspopup="true"
        aria-expanded={open}
        onClick={() => setOpen((value) => !value)}
      >
        <BellIcon />
        {unreadCount > 0 ? (
          <span className={badgeClassName} aria-hidden="true">
            {badge}
          </span>
        ) : null}
      </button>
      {open ? (
        // biome-ignore lint/a11y/useSemanticElements: non-modal popover panel anchored to the bell button
        <div className={styles.panel} role="dialog" aria-label="알림">
          <div className={styles.panelHead}>
            <strong>알림</strong>
            {memberToken && items.some((item) => item.unread) ? (
              <button type="button" onClick={() => void readAll()}>
                모두 읽음
              </button>
            ) : null}
          </div>
          {error ? <p className={styles.error}>{error}</p> : null}
          {loading && items.length === 0 ? (
            <p className={styles.status}>알림을 불러오는 중입니다.</p>
          ) : null}
          {!loading && !error && items.length === 0 ? (
            <p className={styles.status}>새 알림이 없습니다.</p>
          ) : null}
          {items.length > 0 ? (
            <ul className={styles.list}>
              {items.map((item) => (
                <li key={item.key}>
                  <button
                    type="button"
                    className={item.unread ? styles.itemUnread : styles.item}
                    onClick={() => void openItem(item)}
                  >
                    <span className={styles.itemMeta}>
                      <span className={styles.itemLabel}>{item.label}</span>
                      <time dateTime={item.createdAt ?? undefined}>
                        {relativeLabel(item.createdAt)}
                      </time>
                    </span>
                    <span className={styles.itemTitle}>{item.title}</span>
                    <span className={styles.itemMessage}>{item.message}</span>
                  </button>
                </li>
              ))}
            </ul>
          ) : null}
          <div className={styles.panelFoot}>
            <button
              type="button"
              onClick={() => {
                setOpen(false);
                navigate("/glow-alerts");
              }}
            >
              전체 보기
            </button>
          </div>
        </div>
      ) : null}
    </div>
  );
}

export default NotificationBell;
