import { type ReactNode, useCallback, useMemo, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { useAuth } from "../../../auth/AuthContext";
import {
  type AdminSubscriptionOverviewResponse,
  type AdminSubscriptionSchedule,
  fetchAdminSubscriptionOverview,
} from "../../../shared/data/subscriptionAdminApi";
import shellStyles from "../AdminMarketingPage/AdminMarketingPage.module.css";
import { AdminMarketingShell } from "../AdminMarketingPage/AdminMarketingShell";
import shellToolbar from "../AdminUsersPage/AdminUsersShellToolbar.module.css";
import styles from "./AdminSubscriptionsPage.module.css";
import { DispatchesTab } from "./DispatchesTab";
import { MailLogsTab } from "./MailLogsTab";
import { SchedulesTab } from "./SchedulesTab";
import { SubscribersTab } from "./SubscribersTab";
import { useAdminQuery, useUrlParams } from "./adminSubscriptionShared";

type SubscriptionTab = "subscribers" | "schedules" | "dispatches" | "mail-logs";

const TABS: ReadonlyArray<{
  readonly value: SubscriptionTab;
  readonly label: string;
}> = [
  { value: "subscribers", label: "구독자" },
  { value: "schedules", label: "발송 스케줄" },
  { value: "dispatches", label: "발송 이력" },
  { value: "mail-logs", label: "메일 기록" },
];

const SHELL_CLASS_NAMES = {
  toolbarCard: shellToolbar.toolbarCard,
  header: shellToolbar.header,
  title: shellToolbar.title,
  subtitle: shellToolbar.subtitle,
  statusText: shellToolbar.statusText,
  stats: shellToolbar.stats,
  statCard: shellToolbar.statCard,
  statLabel: shellToolbar.statLabel,
  statValue: shellToolbar.statValue,
};

const StatCount = ({
  n,
  unit,
}: { readonly n: number; readonly unit: string }) => (
  <>
    <span className={shellToolbar.statNumber}>{n.toLocaleString("ko-KR")}</span>
    <span className={shellToolbar.statUnit}>{unit}</span>
  </>
);

export function SubscriptionAdminFrame({
  title,
  description = "",
  stats = [],
  children,
}: {
  readonly title: string;
  readonly description?: string;
  readonly stats?: ReadonlyArray<{
    readonly label: string;
    readonly value: ReactNode;
  }>;
  readonly children: ReactNode;
}) {
  const { accessToken, authReady, user } = useAuth();

  if (!authReady) {
    return (
      <main className={shellStyles.page}>
        <div className={styles.gateMessage}>
          관리자 권한을 확인하는 중입니다.
        </div>
      </main>
    );
  }

  if (!accessToken || user?.role !== "ADMIN") {
    return (
      <main className={shellStyles.page}>
        <div className={styles.gateMessage}>
          관리자만 구독 관리 화면을 확인할 수 있습니다.
        </div>
      </main>
    );
  }

  return (
    <AdminMarketingShell
      currentPath="/admin/subscriptions"
      title={title}
      description={description}
      classNames={SHELL_CLASS_NAMES}
      stats={stats}
    >
      {children}
    </AdminMarketingShell>
  );
}

export function BackLink({
  fallback,
  label,
}: {
  readonly fallback: string;
  readonly label: string;
}) {
  const location = useLocation();
  const from = (location.state as { from?: unknown } | null)?.from;
  const to =
    typeof from === "string" && from.startsWith("/admin/subscriptions")
      ? from
      : fallback;
  return (
    <Link to={to} className={styles.backLink}>
      <span aria-hidden="true">←</span> {label}
    </Link>
  );
}

function SubscriptionsContent() {
  const { params, reset } = useUrlParams();
  const requested = params.get("tab");
  const tab: SubscriptionTab =
    TABS.find((item) => item.value === requested)?.value ?? "subscribers";

  const overviewQuery = useAdminQuery(fetchAdminSubscriptionOverview);
  const [savedSchedules, setSavedSchedules] = useState<
    Record<string, AdminSubscriptionSchedule>
  >({});

  const overview = useMemo<AdminSubscriptionOverviewResponse | null>(() => {
    const base = overviewQuery.data;
    if (!base) return null;
    const merged = base.schedules.map(
      (schedule) => savedSchedules[schedule.contentType] ?? schedule,
    );
    for (const saved of Object.values(savedSchedules)) {
      if (
        !merged.some((schedule) => schedule.contentType === saved.contentType)
      ) {
        merged.push(saved);
      }
    }
    return { ...base, schedules: merged };
  }, [overviewQuery.data, savedSchedules]);

  const handleScheduleSaved = useCallback(
    (schedule: AdminSubscriptionSchedule) => {
      setSavedSchedules((current) => ({
        ...current,
        [schedule.contentType]: schedule,
      }));
    },
    [],
  );

  const activeScheduleCount =
    overview?.schedules.filter(
      (schedule) =>
        schedule.active &&
        (schedule.contentType === "NEWSLETTER" ||
          schedule.contentType === "PRODUCT_RECOMMENDATION"),
    ).length ?? 0;

  const selectTab = (next: SubscriptionTab) => {
    if (next === tab) return;
    reset(["size"], { tab: next === "subscribers" ? null : next });
  };

  return (
    <SubscriptionAdminFrame
      title="구독 관리"
      stats={[
        {
          label: "활성 구독자",
          value: <StatCount n={overview?.totalActive ?? 0} unit="명" />,
        },
        {
          label: "전체 구독자",
          value: <StatCount n={overview?.totalSubscribers ?? 0} unit="명" />,
        },
        {
          label: "누적 발송",
          value: <StatCount n={overview?.totalDispatches ?? 0} unit="회" />,
        },
        {
          label: "사용 중인 스케줄",
          value: <StatCount n={activeScheduleCount} unit="개" />,
        },
      ]}
    >
      <section className={styles.panel}>
        <div
          className={styles.tabBar}
          role="tablist"
          aria-label="구독 관리 메뉴"
        >
          {TABS.map((item) => (
            <button
              key={item.value}
              type="button"
              role="tab"
              aria-selected={tab === item.value}
              className={tab === item.value ? styles.tabActive : styles.tab}
              onClick={() => selectTab(item.value)}
            >
              {item.label}
            </button>
          ))}
        </div>
        {tab === "subscribers" ? <SubscribersTab /> : null}
        {tab === "schedules" ? (
          <SchedulesTab
            key={overviewQuery.data ? "ready" : "pending"}
            overview={overview}
            loading={overviewQuery.loading}
            error={overviewQuery.error}
            onSaved={handleScheduleSaved}
          />
        ) : null}
        {tab === "dispatches" ? <DispatchesTab /> : null}
        {tab === "mail-logs" ? <MailLogsTab /> : null}
      </section>
    </SubscriptionAdminFrame>
  );
}

export function AdminSubscriptionsPage() {
  const { accessToken, authReady, user } = useAuth();
  if (!authReady || !accessToken || user?.role !== "ADMIN") {
    return (
      <SubscriptionAdminFrame title="구독 관리">{null}</SubscriptionAdminFrame>
    );
  }
  return <SubscriptionsContent />;
}

export default AdminSubscriptionsPage;
