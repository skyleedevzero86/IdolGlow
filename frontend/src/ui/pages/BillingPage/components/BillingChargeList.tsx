import type { TossBillingCharge } from '../../../../shared/data/tossBilling/types/TossBillingCharge';
import type { TossBillingMethod } from '../../../../shared/data/tossBilling/types/TossBillingMethod';
import { formatIdolGlowDateTime } from '../../../../shared/format/idolGlowDateTime';
import styles from '../BillingPage.module.css';

type BillingChargeListProps = {
  readonly charges: readonly TossBillingCharge[];
  readonly methods: readonly TossBillingMethod[];
  readonly selectedMethodId: number | null;
  readonly busy: boolean;
  readonly onRetryThisCycle: (chargeId: number) => void;
  readonly onRetryAndSetDefault: (chargeId: number) => void;
};

export function BillingChargeList({
  charges,
  methods,
  selectedMethodId,
  busy,
  onRetryThisCycle,
  onRetryAndSetDefault,
}: BillingChargeListProps) {
  const selected = methods.find(method => method.id === selectedMethodId) ?? null;

  return (
    <section className={styles.card}>
      <h2 className={styles.cardTitle}>자동결제 내역</h2>
      {charges.length === 0 ? (
        <p className={styles.meta}>아직 결제 내역이 없습니다.</p>
      ) : (
        <ul className={styles.chargeList}>
          {charges.map(charge => {
            const unpaid = charge.status === 'FAILED';
            return (
              <li key={charge.id} className={styles.chargeItem}>
                <p className={styles.chargeTitle}>
                  {charge.orderName} · {charge.amount.toLocaleString('ko-KR')}원 · {charge.status}
                </p>
                <p className={styles.chargeMeta}>
                  {charge.orderId}
                  {charge.approvedAt ? ` · ${formatIdolGlowDateTime(charge.approvedAt)}` : ''}
                  {charge.failMessage ? ` · ${charge.failMessage}` : ''}
                </p>
                {charge.attempts.length > 0 ? (
                  <ul className={styles.attemptList}>
                    {charge.attempts.map(attempt => (
                      <li key={attempt.id} className={styles.attemptItem}>
                        시도 {attempt.attemptNo}: {attempt.cardCompany ?? '카드'}{' '}
                        {attempt.cardNumber ?? ''} · {attempt.status}
                        {attempt.failMessage ? ` · ${attempt.failMessage}` : ''}
                      </li>
                    ))}
                  </ul>
                ) : null}
                {unpaid ? (
                  <div className={styles.rowActions}>
                    <button
                      type="button"
                      className={styles.primaryButton}
                      disabled={busy || !selected}
                      onClick={() => onRetryThisCycle(charge.id)}
                    >
                      {selected
                        ? `${selected.cardCompany ?? '선택 카드'}로 미납만 결제`
                        : '위에서 카드를 선택한 뒤 미납 결제'}
                    </button>
                    <button
                      type="button"
                      className={styles.secondaryButton}
                      disabled={busy || !selected}
                      onClick={() => onRetryAndSetDefault(charge.id)}
                    >
                      미납 결제 + 기본 카드도 변경
                    </button>
                  </div>
                ) : null}
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}
