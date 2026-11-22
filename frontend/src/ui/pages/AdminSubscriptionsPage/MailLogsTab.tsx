import { useCallback } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import {
  type MailSendKind,
  type MailSendStatus,
  fetchAdminMailLogs,
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

const MAIL_KIND_OPTIONS: readonly Option<MailSendKind>[] = [
  { value: "PRODUCT_RECOMMENDATION", label: "상품 추천" },
  { value: "NEWSLETTER", label: "뉴스레터" },
  { value: "IDOLGLOW", label: "웹진" },
  { value: "UNSUBSCRIBE_CONFIRM", label: "수신 거부 확인" },
  { value: "CONSENT_REMINDER", label: "수신 동의 안내" },
  { value: "AUTH", label: "인증" },
  { value: "PAYMENT", label: "결제 안내" },
  { value: "OTHER", label: "기타" },
];

export const MAIL_STATUS_OPTIONS: readonly Option<MailSendStatus>[] = [
  { value: "SENT", label: "발송" },
  { value: "FAILED", label: "실패" },
  { value: "SKIPPED", label: "건너뜀" },
];

export function MailLogsTab() {
  const { params, update, reset } = useUrlParams();
  const navigate = useNavigate();
  const location = useLocation();
  const keyword = params.get("q") ?? "";
  const kind = parseOption(params.get("kind"), MAIL_KIND_OPTIONS);
  const status = parseOption(params.get("status"), MAIL_STATUS_OPTIONS);
  const from = parseDate(params.get("from"));
  const to = parseDate(params.get("to"));
  const pageIndex = parsePageIndex(params.get("page"));
  const pageSize = parsePageSize(params.get("size"));

  const load = useCallback(
    (token: string, signal: AbortSignal) =>
      fetchAdminMailLogs(
        token,
        { keyword, kind, status, from, to, page: pageIndex, size: pageSize },
        signal,
      ),
    [keyword, kind, status, from, to, pageIndex, pageSize],
  );
  const { data, loading, error } = useAdminQuery(load);
  const items = data?.items ?? [];
  const backState = { from: `${location.pathname}${location.search}` };
  const detailPath = (id: number) => `/admin/subscriptions/mail-logs/${id}`;

  return (
    <div className={styles.tabBody}>
      <FilterBar
        keyword={keyword}
        keywordPlaceholder="수신 이메일 검색"
        onKeywordSubmit={(q) => update({ q })}
        onReset={() => reset(["tab", "size"])}
        canReset={Boolean(keyword || kind || status || from || to)}
      >
        <FilterSelect
          label="메일 종류"
          allLabel="전체 종류"
          value={kind}
          options={MAIL_KIND_OPTIONS}
          onChange={(value) => update({ kind: value })}
        />
        <FilterSelect
          label="발송 상태"
          allLabel="전체 상태"
          value={status}
          options={MAIL_STATUS_OPTIONS}
          onChange={(value) => update({ status: value })}
        />
        <DateRangeFilter from={from} to={to} onChange={update} />
      </FilterBar>
      <p className={styles.helperText}>
        메일 기록은 90일이 지나면 자동으로 지워집니다.
      </p>
      <ListStatus loading={loading} error={error} hasData={data !== null} />
      <div className={styles.tableScroll}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>시각</th>
              <th>종류</th>
              <th>수신자</th>
              <th className={styles.thLeft}>제목</th>
              <th>상태</th>
              <th className={styles.thLeft}>오류</th>
            </tr>
          </thead>
          <tbody>
            {loading && data === null ? (
              <TableMessageRow colSpan={6}>불러오는 중입니다.</TableMessageRow>
            ) : items.length === 0 ? (
              <TableMessageRow colSpan={6}>
                {error
                  ? "목록을 불러오지 못했습니다."
                  : "조건에 맞는 메일 기록이 없습니다."}
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
                  <td className={styles.tdTime}>
                    {item.sentAt ?? item.createdAt}
                  </td>
                  <td>{item.kindLabel}</td>
                  <td>{item.recipientEmail}</td>
                  <td className={styles.tdLeft}>
                    <Link
                      to={detailPath(item.id)}
                      state={backState}
                      className={styles.cellLink}
                      onClick={(event) => event.stopPropagation()}
                    >
                      {item.subject || "(제목 없음)"}
                    </Link>
                  </td>
                  <td>
                    <StatusBadge
                      status={item.status}
                      label={item.statusLabel}
                    />
                  </td>
                  <td
                    className={styles.tdError}
                    title={item.error ?? undefined}
                  >
                    {item.error ?? "-"}
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
        onPageChange={(next) => update({ page: next + 1 })}
        onSizeChange={(size) => update({ size })}
      />
    </div>
  );
}
