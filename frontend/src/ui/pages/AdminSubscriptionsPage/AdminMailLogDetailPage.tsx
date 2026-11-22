import { useCallback } from "react";
import { Link, useParams } from "react-router-dom";
import { fetchAdminMailLog } from "../../../shared/data/subscriptionAdminApi";
import { BackLink, SubscriptionAdminFrame } from "./AdminSubscriptionsPage";
import styles from "./AdminSubscriptionsPage.module.css";
import {
  MailBodyPreview,
  StatusBadge,
  useAdminQuery,
} from "./adminSubscriptionShared";

const LIST_PATH = "/admin/subscriptions?tab=mail-logs";

function MailLogDetail({ mailLogId }: { readonly mailLogId: number }) {
  const load = useCallback(
    (token: string, signal: AbortSignal) =>
      fetchAdminMailLog(token, mailLogId, signal),
    [mailLogId],
  );
  const { data, error } = useAdminQuery(load);

  if (!data) {
    return (
      <section className={styles.panel}>
        <div className={styles.detailBody}>
          <BackLink fallback={LIST_PATH} label="메일 기록" />
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

  return (
    <>
      <section className={styles.panel}>
        <div className={styles.detailBody}>
          <BackLink fallback={LIST_PATH} label="메일 기록" />
          <div className={styles.detailHead}>
            <div>
              <span className={styles.detailEyebrow}>{data.kindLabel}</span>
              <h2 className={styles.detailTitle}>
                {data.subject || "(제목 없음)"}
              </h2>
            </div>
            <StatusBadge status={data.status} label={data.statusLabel} />
          </div>
          <dl className={styles.metaGrid}>
            <div>
              <dt>수신자</dt>
              <dd>{data.recipientEmail}</dd>
            </div>
            <div>
              <dt>발송 시각</dt>
              <dd>{data.sentAt ?? "-"}</dd>
            </div>
            <div>
              <dt>기록 시각</dt>
              <dd>{data.createdAt}</dd>
            </div>
            <div>
              <dt>연결된 발송</dt>
              <dd>
                {data.dispatchId ? (
                  <Link
                    to={`/admin/subscriptions/dispatches/${data.dispatchId}`}
                    className={styles.textLink}
                  >
                    #{data.dispatchId} 보기
                  </Link>
                ) : (
                  "-"
                )}
              </dd>
            </div>
          </dl>
          {data.error ? (
            <p className={styles.errorBox}>
              <strong>오류</strong> {data.error}
            </p>
          ) : null}
        </div>
      </section>

      <section className={styles.panel}>
        <header className={styles.panelHead}>
          <h3 className={styles.panelTitle}>본문</h3>
        </header>
        <div className={styles.detailBody}>
          <MailBodyPreview
            htmlBody={data.htmlBody}
            textBody={data.textBody}
            kind={data.kind}
          />
        </div>
      </section>
    </>
  );
}

export function AdminMailLogDetailPage() {
  const { mailLogId } = useParams();
  const id = Number(mailLogId);
  const valid = Number.isInteger(id) && id > 0;

  return (
    <SubscriptionAdminFrame title="메일 기록 상세">
      {valid ? (
        <MailLogDetail key={id} mailLogId={id} />
      ) : (
        <section className={styles.panel}>
          <div className={styles.detailBody}>
            <BackLink fallback={LIST_PATH} label="메일 기록" />
            <p className={styles.inlineError}>잘못된 메일 기록 번호입니다.</p>
          </div>
        </section>
      )}
    </SubscriptionAdminFrame>
  );
}

export default AdminMailLogDetailPage;
