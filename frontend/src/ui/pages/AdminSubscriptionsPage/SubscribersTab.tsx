import { useCallback, useMemo } from "react";
import {
  type EmailSubscriberAudience,
  type RecommendationBasis,
  type SubscriberStatusFilter,
  fetchAdminEmailSubscribers,
  fetchAdminRecommendationSubscribers,
} from "../../../shared/data/subscriptionAdminApi";
import styles from "./AdminSubscriptionsPage.module.css";
import {
  FilterBar,
  FilterSelect,
  ListFooter,
  ListStatus,
  type Option,
  StatusBadge,
  TableMessageRow,
  parseOption,
  parsePageIndex,
  parsePageSize,
  useAdminQuery,
  useUrlParams,
} from "./adminSubscriptionShared";

type SubscriberView = "email" | "recommendation";

const VIEW_OPTIONS: readonly Option<SubscriberView>[] = [
  { value: "email", label: "소식지·웹진" },
  { value: "recommendation", label: "상품 추천 메일" },
];

const STATUS_OPTIONS: readonly Option<SubscriberStatusFilter>[] = [
  { value: "ACTIVE", label: "구독 중" },
  { value: "INACTIVE", label: "해지" },
];

const AUDIENCE_OPTIONS: readonly Option<EmailSubscriberAudience>[] = [
  { value: "NEWSLETTER", label: "소식지" },
  { value: "IDOLGLOW_ISSUE", label: "웹진(호별보기)" },
];

const BASIS_OPTIONS: readonly Option<RecommendationBasis>[] = [
  { value: "AUTO", label: "취향 우선" },
  { value: "POPULAR", label: "인기순" },
];

const statusKey = (active: boolean) => (active ? "ACTIVE" : "INACTIVE");

function EmailSubscriberList() {
  const { params, update, reset } = useUrlParams();
  const keyword = params.get("q") ?? "";
  const audience = parseOption(params.get("audience"), AUDIENCE_OPTIONS);
  const status = parseOption(params.get("status"), STATUS_OPTIONS);
  const pageIndex = parsePageIndex(params.get("page"));
  const pageSize = parsePageSize(params.get("size"));

  const load = useCallback(
    (token: string, signal: AbortSignal) =>
      fetchAdminEmailSubscribers(
        token,
        { keyword, audience, status, page: pageIndex, size: pageSize },
        signal,
      ),
    [keyword, audience, status, pageIndex, pageSize],
  );
  const { data, loading, error } = useAdminQuery(load);
  const items = data?.items ?? [];

  return (
    <>
      <FilterBar
        keyword={keyword}
        keywordPlaceholder="이메일 검색"
        onKeywordSubmit={(q) => update({ q })}
        onReset={() => reset(["tab", "view", "size"])}
        canReset={Boolean(keyword || audience || status)}
      >
        <FilterSelect
          label="구독 대상"
          allLabel="전체 대상"
          value={audience}
          options={AUDIENCE_OPTIONS}
          onChange={(value) => update({ audience: value })}
        />
        <FilterSelect
          label="구독 상태"
          allLabel="전체 상태"
          value={status}
          options={STATUS_OPTIONS}
          onChange={(value) => update({ status: value })}
        />
      </FilterBar>
      <ListStatus loading={loading} error={error} hasData={data !== null} />
      <div className={styles.tableScroll}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th className={styles.thLeft}>이메일</th>
              <th>구독 대상</th>
              <th>상태</th>
              <th>유입 경로</th>
              <th>동의일</th>
              <th>구독일</th>
            </tr>
          </thead>
          <tbody>
            {loading && data === null ? (
              <TableMessageRow colSpan={6}>불러오는 중입니다.</TableMessageRow>
            ) : items.length === 0 ? (
              <TableMessageRow colSpan={6}>
                {error
                  ? "목록을 불러오지 못했습니다."
                  : "조건에 맞는 구독자가 없습니다."}
              </TableMessageRow>
            ) : (
              items.map((item) => (
                <tr key={item.id}>
                  <td className={styles.tdLeft}>{item.email}</td>
                  <td>{item.subscribedTargetLabels.join(", ") || "-"}</td>
                  <td>
                    <StatusBadge
                      status={statusKey(item.active)}
                      label={item.statusLabel}
                    />
                  </td>
                  <td>{item.source || "-"}</td>
                  <td className={styles.tdTime}>{item.consentedAt}</td>
                  <td className={styles.tdTime}>{item.subscribedAt}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
      <ListFooter
        page={data}
        pageIndex={pageIndex}
        pageSize={pageSize}
        unit="명"
        onPageChange={(next) => update({ page: next + 1 })}
        onSizeChange={(size) => update({ size })}
      />
    </>
  );
}

function RecommendationSubscriberList() {
  const { params, update, reset } = useUrlParams();
  const keyword = params.get("q") ?? "";
  const status = parseOption(params.get("status"), STATUS_OPTIONS);
  const basis = parseOption(params.get("basis"), BASIS_OPTIONS);
  const pageIndex = parsePageIndex(params.get("page"));
  const pageSize = parsePageSize(params.get("size"));

  const load = useCallback(
    (token: string, signal: AbortSignal) =>
      fetchAdminRecommendationSubscribers(
        token,
        { keyword, status, basis, page: pageIndex, size: pageSize },
        signal,
      ),
    [keyword, status, basis, pageIndex, pageSize],
  );
  const { data, loading, error } = useAdminQuery(load);
  const items = data?.items ?? [];

  return (
    <>
      <FilterBar
        keyword={keyword}
        keywordPlaceholder="이메일·닉네임 검색"
        onKeywordSubmit={(q) => update({ q })}
        onReset={() => reset(["tab", "view", "size"])}
        canReset={Boolean(keyword || status || basis)}
      >
        <FilterSelect
          label="구독 상태"
          allLabel="전체 상태"
          value={status}
          options={STATUS_OPTIONS}
          onChange={(value) => update({ status: value })}
        />
        <FilterSelect
          label="추천 기준"
          allLabel="전체 기준"
          value={basis}
          options={BASIS_OPTIONS}
          onChange={(value) => update({ basis: value })}
        />
      </FilterBar>
      <ListStatus loading={loading} error={error} hasData={data !== null} />
      <div className={styles.tableScroll}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th className={styles.thLeft}>회원</th>
              <th>상태</th>
              <th>추천 기준</th>
              <th>동의 경로</th>
              <th>동의일</th>
              <th>최근 발송</th>
              <th>해지일</th>
            </tr>
          </thead>
          <tbody>
            {loading && data === null ? (
              <TableMessageRow colSpan={7}>불러오는 중입니다.</TableMessageRow>
            ) : items.length === 0 ? (
              <TableMessageRow colSpan={7}>
                {error
                  ? "목록을 불러오지 못했습니다."
                  : "조건에 맞는 구독자가 없습니다."}
              </TableMessageRow>
            ) : (
              items.map((item) => (
                <tr key={item.id}>
                  <td className={styles.tdLeft}>
                    <span className={styles.cellTitle}>
                      {item.nickname || "-"}
                    </span>
                    <span className={styles.cellSub}>{item.email}</span>
                  </td>
                  <td>
                    <StatusBadge
                      status={statusKey(item.active)}
                      label={item.statusLabel}
                    />
                  </td>
                  <td>{item.preferredBasisLabel}</td>
                  <td>{item.consentSourceLabel}</td>
                  <td className={styles.tdTime}>{item.consentedAt}</td>
                  <td className={styles.tdTime}>{item.lastSentAt ?? "-"}</td>
                  <td className={styles.tdTime}>
                    {item.unsubscribedAt ?? "-"}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
      <ListFooter
        page={data}
        pageIndex={pageIndex}
        pageSize={pageSize}
        unit="명"
        onPageChange={(next) => update({ page: next + 1 })}
        onSizeChange={(size) => update({ size })}
      />
    </>
  );
}

export function SubscribersTab() {
  const { params, reset } = useUrlParams();
  const view = useMemo<SubscriberView>(
    () =>
      params.get("view") === "recommendation" ? "recommendation" : "email",
    [params],
  );

  const switchView = (next: SubscriberView) => {
    if (next === view) return;
    reset(["tab", "size"], {
      view: next === "recommendation" ? next : null,
    });
  };

  return (
    <div className={styles.tabBody}>
      <div className={styles.segment} role="tablist" aria-label="구독자 종류">
        {VIEW_OPTIONS.map((option) => (
          <button
            key={option.value}
            type="button"
            role="tab"
            aria-selected={view === option.value}
            className={
              view === option.value ? styles.segmentActive : styles.segmentItem
            }
            onClick={() => switchView(option.value)}
          >
            {option.label}
          </button>
        ))}
      </div>
      {view === "email" ? (
        <EmailSubscriberList key="email" />
      ) : (
        <RecommendationSubscriberList key="recommendation" />
      )}
    </div>
  );
}
