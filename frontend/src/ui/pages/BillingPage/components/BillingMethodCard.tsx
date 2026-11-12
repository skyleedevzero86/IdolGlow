import type { TossBillingMethod } from '../../../../shared/data/tossBilling/types/TossBillingMethod';
import styles from '../BillingPage.module.css';

type BillingMethodCardProps = {
  readonly methods: readonly TossBillingMethod[];
  readonly selectedMethodId: number | null;
  readonly hasUnpaid: boolean;
  readonly busy: boolean;
  readonly onSelect: (methodId: number) => void;
  readonly onRegister: (setAsDefault: boolean) => void;
  readonly onChangeDefault: (methodId: number) => void;
  readonly onDeactivate: (methodId: number) => void;
};

export function BillingMethodCard({
  methods,
  selectedMethodId,
  hasUnpaid,
  busy,
  onSelect,
  onRegister,
  onChangeDefault,
  onDeactivate,
}: BillingMethodCardProps) {
  return (
    <section className={styles.card}>
      <h2 className={styles.cardTitle}>결제 수단</h2>
      <p className={styles.meta}>
        한도 부족 같은 실패가 없어도 기본 카드는 언제든 바꿀 수 있습니다. 바꾸면 다음 정기결제부터 새
        카드로 청구됩니다.
      </p>
      {methods.length === 0 ? (
        <p className={styles.meta}>등록된 카드가 없습니다. 토스 카드 등록창에서 카드를 등록하세요.</p>
      ) : (
        <ul className={styles.chargeList}>
          {methods.map(method => {
            const selected = method.id === selectedMethodId;
            return (
              <li key={method.id} className={styles.chargeItem}>
                <p className={styles.chargeTitle}>
                  {method.cardCompany ?? '카드'} · {method.cardNumber ?? '번호 없음'}
                  {method.isDefault ? ' · 기본 카드' : ''}
                </p>
                <p className={styles.chargeMeta}>
                  {[method.cardType, method.ownerType].filter(Boolean).join(' · ') || '카드 정보'}
                  {hasUnpaid && selected ? ' · 미납 재결제용으로 선택됨' : ''}
                </p>
                <div className={styles.rowActions}>
                  {method.isDefault ? (
                    <button type="button" className={styles.secondaryButton} disabled>
                      현재 기본 카드
                    </button>
                  ) : (
                    <button
                      type="button"
                      className={styles.primaryButton}
                      disabled={busy}
                      onClick={() => onChangeDefault(method.id)}
                    >
                      이 카드로 변경
                    </button>
                  )}
                  {hasUnpaid ? (
                    <button
                      type="button"
                      className={styles.secondaryButton}
                      disabled={busy || selected}
                      onClick={() => onSelect(method.id)}
                    >
                      미납 결제용 선택
                    </button>
                  ) : null}
                  <button
                    type="button"
                    className={styles.dangerButton}
                    disabled={busy || (methods.length === 1 && method.isDefault)}
                    onClick={() => onDeactivate(method.id)}
                  >
                    삭제
                  </button>
                </div>
              </li>
            );
          })}
        </ul>
      )}
      <div className={styles.rowActions}>
        <button type="button" className={styles.primaryButton} onClick={() => onRegister(true)} disabled={busy}>
          {methods.length === 0 ? '카드 등록하기' : '새 카드 등록하고 기본으로 변경'}
        </button>
        {methods.length > 0 ? (
          <button type="button" className={styles.secondaryButton} onClick={() => onRegister(false)} disabled={busy}>
            카드만 추가 (기본 유지)
          </button>
        ) : null}
      </div>
    </section>
  );
}
