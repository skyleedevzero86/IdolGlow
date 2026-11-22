import { useCallback } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import {
  type SubscriptionContentType,
  type SubscriptionDispatchStatus,
  fetchAdminSubscriptionDispatches,
} from "../../../shared/data/subscriptionAdminApi";
import styles from "./AdminSubscriptionsPage.module.css";
import {
  DateRangeFilter,
  FilterBar,
  FilterSelect,
  ListFooter,
  ListStatus,
  type Option,
  StatusBadge,
  TableMessageRow,
  parseDate,
  parseOption,
  parsePageIndex,
  parsePageSize,
  useAdminQuery,
  useUrlParams,
} from "./adminSubscriptionShared";

export const CONTENT_TYPE_OPTIONS: readonly Option<SubscriptionContentType>[] =
  [
    { value: "NEWSLETTER", label: "뉴스레터" },
    { value: "IDOLGLOW_ISSUE", label: "웹진" },
    { value: "PRODUCT_RECOMMENDATION", label: "상품 추천" },
  ];

const DISPATCH_STATUS_OPTIONS: readonly Option<SubscriptionDispatchStatus>[] = [
  { value: "RECORDED", label: "기록됨" },
  { value: "SENDING", label: "발송중" },
  { value: "SENT", label: "발송완료" },
  { value: "PARTIAL_FAILED", label: "일부실패" },
  { value: "FAILED", label: "발송실패" },
];

export function DispatchesTab() {
  const { params, update, reset } = useUrlParams();
  const navigate = useNavigate();
  const location = useLocation();
  const keyword = params.get("q") ?? "";
  const contentType = parseOption(params.get("type"), CONTENT_TYPE_OPTIONS);
  const status = parseOption(params.get("status"), DISPATCH_STATUS_OPTIONS);
  const from = parseDate(params.get("from"));
  const to = parseDate(params.get("to"));
  const pageIndex = parsePageIndex(params.get("page"));
  const pageSize = parsePageSize(params.get("size"));

  const load = useCallback(
    (token: string, signal: AbortSignal) =>
      fetchAdminSubscriptionDispatches(
        token,
        {
          keyword,
          contentType,
          status,
          from,
          to,
          page: pageIndex,
          size: pageSize,
        },
        signal,
      ),
    [keyword, contentType, status, from, to, pageIndex, pageSize],
  );
  const { data, loading, error } = useAdminQuery(load);
  const items = data?.items ?? [];
  const backState = { from: `${location.pathname}${location.search}` };
  const detailPath = (id: number) => `/admin/subscriptions/dispatches/${id}`;

  return (
    <div className={styles.tabBody}>
      <FilterBar
        keyword={keyword}
        keywordPlaceholder="제목·슬러그 검색"
        onKeywordSubmit={(q) => update({ q })}
        onReset={() => reset(["tab", "size"])}
        canReset={Boolean(keyword || contentType || status || from || to)}
      >
        <FilterSelect
          label="콘텐츠 유형"
          allLabel="전체 유형"
          value={contentType}
          options={CONTENT_TYPE_OPTIONS}
          onChange={(value) => update({ type: value })}
        />
        <FilterSelect
          label="발송 상태"
          allLabel="전체 상태"
          value={status}
          options={DISPATCH_STATUS_OPTIONS}
          onChange={(value) => update({ status: value })}
        />
        <DateRangeFilter from={from} to={to} onChange={update} />
      </FilterBar>
      <ListStatus loading={loading} error={error} hasData={data !== null} />
      <div className={styles.tableScroll}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>발송 일시</th>
              <th>유형</th>
              <th className={styles.thLeft}>콘텐츠</th>
              <th>상태</th>
              <th>대상</th>
              <th>발송</th>
              <th>실패</th>
              <th>건너뜀</th>
            </tr>
          </thead>
          <tbody>
            {loading && data === null ? (
              <TableMessageRow colSpan={8}>불러오는 중입니다.</TableMessageRow>
            ) : items.length === 0 ? (
              <TableMessageRow colSpan={8}>
                {error
                  ? "목록을 불러오지 못했습니다."
                  : "조건에 맞는 발송 이력이 없습니다."}
              </TableMessageRow>
            ) : (
              items.map((item) => (
                <tr
                  key={item.id}
                  className={styles.rowLink}
                  onClick={() =>
                    navigate(detailPath(item.id), { state: backState })
                  }
                >
                  <td className={styles.tdTime}>{item.dispatchedAt}</td>
                  <td>{item.contentTypeLabel}</td>
                  <td className={styles.tdLeft}>
                    <Link
                      to={detailPath(item.id)}
                      state={backState}
                      className={styles.cellLink}
                      onClick={(event) => event.stopPropagation()}
                    >
                      {item.contentTitle}
                    </Link>
                    <span className={styles.cellSub}>{item.contentSlug}</span>
                  </td>
                  <td>
                    <StatusBadge
                      status={item.dispatchStatus}
                      label={item.dispatchStatusLabel}
                    />
                  </td>
                  <td className={styles.tdNum}>{item.recipientCount}</td>
                  <td className={styles.tdNum}>{item.sentCount}</td>
                  <td
                    className={
                      item.failedCount > 0 ? styles.tdNumDanger : styles.tdNum
                    }
                  >
                    {item.failedCount}
                  </td>
                  <td className={styles.tdNum}>{item.skippedCount}</td>
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
        onPageChange={(next) => update({ page: next + 1 })}
        onSizeChange={(size) => update({ size })}
      />
    </div>
  );
}
