import type { TossBillingSubscription } from '../../../../shared/data/tossBilling/types/TossBillingSubscription';
import { formatIdolGlowDateTime } from '../../../../shared/format/idolGlowDateTime';
import styles from '../BillingPage.module.css';

type BillingSubscriptionCardProps = {
  readonly subscription: TossBillingSubscription | null;
  readonly hasMethod: boolean;
  readonly busy: boolean;
  readonly onStart: () => void;
  readonly onCancel: () => void;
  readonly onWithdrawCancel: () => void;
  readonly onChargeNow: () => void;
};

export function BillingSubscriptionCard({
  subscription,
  hasMethod,
  busy,
  onStart,
  onCancel,
  onWithdrawCancel,
  onChargeNow,
}: BillingSubscriptionCardProps) {
  const active = subscription?.status === 'ACTIVE';
  const cancelScheduled = subscription?.status === 'CANCEL_SCHEDULED';
  const paused = subscription?.status === 'PAUSED';

  return (
    <section className={styles.card}>
      <h2 className={styles.cardTitle}>Glow Pass (월간 구독)</h2>
      {subscription ? (
        <p className={styles.meta}>
          {subscription.orderName} · {subscription.amount.toLocaleString('ko-KR')}원 · {subscription.status}
          <br />
          다음 결제일: {formatIdolGlowDateTime(subscription.nextBillingAt)}
          <br />
          최근 결제: {formatIdolGlowDateTime(subscription.lastChargedAt)}
          {subscription.failCount > 0 ? (
            <>
              <br />
              실패 {subscription.failCount}회 · 재시도: {formatIdolGlowDateTime(subscription.retryAt)}
            </>
          ) : null}
          {cancelScheduled ? (
            <>
              <br />
              해지 예약 · 이용 종료: {formatIdolGlowDateTime(subscription.endsAt)}
            </>
          ) : null}
          {paused ? (
            <>
              <br />
              결제가 멈춰 혜택이 중지되었습니다. 카드를 다시 등록하면 재개됩니다.
            </>
          ) : null}
        </p>
      ) : (
        <p className={styles.meta}>구독이 없습니다. 카드 등록 후 Glow Pass를 시작할 수 있습니다.</p>
      )}
      <div className={styles.rowActions}>
        <button type="button" className={styles.primaryButton} onClick={onStart} disabled={busy || !hasMethod}>
          {active || cancelScheduled ? '구독 갱신/즉시결제 포함 시작' : 'Glow Pass 시작'}
        </button>
        <button type="button" className={styles.secondaryButton} onClick={onChargeNow} disabled={busy || !hasMethod}>
          지금 테스트 결제
        </button>
        {cancelScheduled ? (
          <button type="button" className={styles.secondaryButton} onClick={onWithdrawCancel} disabled={busy}>
            해지 철회
          </button>
        ) : (
          <button type="button" className={styles.dangerButton} onClick={onCancel} disabled={busy || (!active && !paused)}>
            해지 예약
          </button>
        )}
      </div>
    </section>
  );
}
