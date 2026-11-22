import { useCallback } from "react";
import { Link, useParams } from "react-router-dom";
import {
  type DispatchRecipientStatus,
  fetchAdminSubscriptionDispatchDetail,
} from "../../../shared/data/subscriptionAdminApi";
import { BackLink, SubscriptionAdminFrame } from "./AdminSubscriptionsPage";
import styles from "./AdminSubscriptionsPage.module.css";
import {
  FilterBar,
  FilterSelect,
  ListFooter,
  ListStatus,
  MailBodyPreview,
  type Option,
  StatusBadge,
  TableMessageRow,
  parseOption,
  parsePageIndex,
  parsePageSize,
  useAdminQuery,
  useUrlParams,
} from "./adminSubscriptionShared";

const RECIPIENT_STATUS_OPTIONS: readonly Option<DispatchRecipientStatus>[] = [
  { value: "SENT", label: "발송" },
  { value: "FAILED", label: "실패" },
  { value: "SKIPPED", label: "건너뜀" },
];

const LIST_PATH = "/admin/subscriptions?tab=dispatches";

function DispatchDetail({ dispatchId }: { readonly dispatchId: number }) {
  const { params, update, reset } = useUrlParams();
  const keyword = params.get("q") ?? "";
  const status = parseOption(params.get("status"), RECIPIENT_STATUS_OPTIONS);
  const pageIndex = parsePageIndex(params.get("page"));
  const pageSize = parsePageSize(params.get("size"));

  const load = useCallback(
    (token: string, signal: AbortSignal) =>
      fetchAdminSubscriptionDispatchDetail(
        token,
        dispatchId,
        { keyword, status, page: pageIndex, size: pageSize },
        signal,
      ),
    [dispatchId, keyword, status, pageIndex, pageSize],
  );
  const { data, loading, error } = useAdminQuery(load);

  if (!data) {
    return (
      <section className={styles.panel}>
        <div className={styles.detailBody}>
          <BackLink fallback={LIST_PATH} label="발송 이력" />
          {error ? (
            <p className={styles.inlineError} role="alert">
              {error}
            </p>
          ) : (
            <p className={styles.inlineInfo}>불러오는 중입니다.</p>
          )}
        </div>
      </section>
    );
  }

  const { dispatch, recipients, mailContent, lastAttemptAt } = data;

  return (
    <>
      <section className={styles.panel}>
        <div className={styles.detailBody}>
          <BackLink fallback={LIST_PATH} label="발송 이력" />
          <div className={styles.detailHead}>
            <div>
              <span className={styles.detailEyebrow}>
                {dispatch.contentTypeLabel} · {dispatch.dispatchChannelLabel}
              </span>
              <h2 className={styles.detailTitle}>{dispatch.contentTitle}</h2>
              {dispatch.contentSummary ? (
                <p className={styles.detailSummary}>
                  {dispatch.contentSummary}
                </p>
              ) : null}
            </div>
            <StatusBadge
              status={dispatch.dispatchStatus}
              label={dispatch.dispatchStatusLabel}
            />
          </div>
          <dl className={styles.metaGrid}>
            <div>
              <dt>발송 일시</dt>
              <dd>{dispatch.dispatchedAt}</dd>
            </div>
            <div>
              <dt>마지막 시도</dt>
              <dd>{lastAttemptAt ?? "-"}</dd>
            </div>
            <div>
              <dt>슬러그</dt>
              <dd>{dispatch.contentSlug}</dd>
            </div>
            <div>
              <dt>발송 번호</dt>
              <dd>#{dispatch.id}</dd>
            </div>
          </dl>
          <div className={styles.countRow}>
            <div className={styles.countCell}>
              <span>대상</span>
              <strong>{dispatch.recipientCount}</strong>
            </div>
            <div className={styles.countCell}>
              <span>발송</span>
              <strong>{dispatch.sentCount}</strong>
            </div>
            <div className={styles.countCell}>
              <span>실패</span>
              <strong
                className={
                  dispatch.failedCount > 0 ? styles.countDanger : undefined
                }
              >
                {dispatch.failedCount}
              </strong>
            </div>
            <div className={styles.countCell}>
              <span>건너뜀</span>
              <strong>{dispatch.skippedCount}</strong>
            </div>
          </div>
        </div>
      </section>

      <section className={styles.panel}>
        <header className={styles.panelHead}>
          <h3 className={styles.panelTitle}>수신자</h3>
        </header>
        <div className={styles.tabBody}>
          <FilterBar
            keyword={keyword}
            keywordPlaceholder="수신 이메일 검색"
            onKeywordSubmit={(q) => update({ q })}
            onReset={() => reset(["size"])}
            canReset={Boolean(keyword || status)}
          >
            <FilterSelect
              label="수신 상태"
              allLabel="전체 상태"
              value={status}
              options={RECIPIENT_STATUS_OPTIONS}
              onChange={(value) => update({ status: value })}
            />
          </FilterBar>
          <ListStatus loading={loading} error={error} hasData />
          <div className={styles.tableScroll}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th className={styles.thLeft}>이메일</th>
                  <th>상태</th>
                  <th>발송 시각</th>
                  <th className={styles.thLeft}>사유</th>
                </tr>
              </thead>
              <tbody>
                {recipients.items.length === 0 ? (
                  <TableMessageRow colSpan={4}>
                    조건에 맞는 수신자가 없습니다.
                  </TableMessageRow>
                ) : (
                  recipients.items.map((recipient) => (
                    <tr key={recipient.id}>
                      <td className={styles.tdLeft}>{recipient.email}</td>
                      <td>
                        <StatusBadge
                          status={recipient.status}
                          label={recipient.statusLabel}
                        />
                      </td>
                      <td className={styles.tdTime}>
                        {recipient.sentAt ?? "-"}
                      </td>
                      <td
                        className={styles.tdError}
                        title={recipient.error ?? undefined}
                      >
                        {recipient.error ?? recipient.skipReasonLabel ?? "-"}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
          <ListFooter
            page={recipients}
            pageIndex={pageIndex}
            pageSize={pageSize}
            unit="명"
            onPageChange={(next) => update({ page: next + 1 })}
            onSizeChange={(size) => update({ size })}
          />
        </div>
      </section>

      <section className={styles.panel}>
        <header className={styles.panelHead}>
          <h3 className={styles.panelTitle}>발송된 메일</h3>
          {mailContent ? (
            <Link
              to={`/admin/subscriptions/mail-logs/${mailContent.mailLogId}`}
              className={styles.textLink}
            >
              메일 기록 보기
            </Link>
          ) : null}
        </header>
        <div className={styles.detailBody}>
          {mailContent ? (
            <>
              <p className={styles.mailSubject}>{mailContent.subject}</p>
              <p className={styles.mailMeta}>
                <StatusBadge
                  status={mailContent.status}
                  label={mailContent.statusLabel}
                />
                <span>{mailContent.sentAt ?? mailContent.createdAt}</span>
              </p>
              <MailBodyPreview
                htmlBody={mailContent.htmlBody}
                textBody={mailContent.textBody}
              />
            </>
          ) : (
            <p className={styles.bodyNotice}>
              이 발송에 연결된 메일 기록이 없습니다.
            </p>
          )}
        </div>
      </section>
    </>
  );
}

export function AdminSubscriptionDispatchDetailPage() {
  const { dispatchId } = useParams();
  const id = Number(dispatchId);
  const valid = Number.isInteger(id) && id > 0;

  return (
    <SubscriptionAdminFrame title="발송 이력 상세">
      {valid ? (
        <DispatchDetail key={id} dispatchId={id} />
      ) : (
        <section className={styles.panel}>
          <div className={styles.detailBody}>
            <BackLink fallback={LIST_PATH} label="발송 이력" />
            <p className={styles.inlineError}>잘못된 발송 번호입니다.</p>
          </div>
        </section>
      )}
    </SubscriptionAdminFrame>
  );
}

export default AdminSubscriptionDispatchDetailPage;
