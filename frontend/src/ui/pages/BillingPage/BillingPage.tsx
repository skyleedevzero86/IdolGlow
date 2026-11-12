import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import { requestBillingAuth } from '../../../shared/data/tossBilling/requestBillingAuth';
import {
  cancelTossBillingSubscription,
  chargeTossBillingNow,
  deactivateTossBillingMethod,
  fetchTossBillingClientConfig,
  fetchTossBillingOverview,
  retryTossBillingCharge,
  setTossBillingDefaultMethod,
  startTossBillingSubscription,
  withdrawTossBillingCancel,
} from '../../../shared/data/tossBilling/tossBillingApi';
import type { TossBillingOverview } from '../../../shared/data/tossBilling/types/TossBillingOverview';
import { BillingChargeList } from './components/BillingChargeList';
import { BillingMethodCard } from './components/BillingMethodCard';
import { BillingSubscriptionCard } from './components/BillingSubscriptionCard';
import styles from './BillingPage.module.css';

const SET_AS_DEFAULT_KEY = 'tossBilling.setAsDefault';

export function BillingPage() {
  const { accessToken, authReady, user } = useAuth();
  const [overview, setOverview] = useState<TossBillingOverview | null>(null);
  const [selectedMethodId, setSelectedMethodId] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const reload = useCallback(async () => {
    if (!accessToken) return;
    setLoading(true);
    setError(null);
    try {
      const next = await fetchTossBillingOverview(accessToken);
      setOverview(next);
      setSelectedMethodId(prev => {
        if (prev && next.methods.some(method => method.id === prev)) return prev;
        return next.method?.id ?? next.methods[0]?.id ?? null;
      });
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : '빌링 정보를 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  }, [accessToken]);

  useEffect(() => {
    if (!authReady || !accessToken || !user) return;
    void reload();
  }, [authReady, accessToken, user, reload]);

  const handleRegisterCard = async (setAsDefault: boolean) => {
    if (!accessToken) return;
    setBusy(true);
    setError(null);
    setMessage(null);
    try {
      sessionStorage.setItem(SET_AS_DEFAULT_KEY, setAsDefault ? 'true' : 'false');
      const config = await fetchTossBillingClientConfig(accessToken);
      await requestBillingAuth({
        config,
        customerEmail: user?.email,
        customerName: user?.nickname ?? user?.email,
      });
    } catch (registerError) {
      setError(registerError instanceof Error ? registerError.message : '카드 등록창을 열지 못했습니다.');
      setBusy(false);
    }
  };

  const handleStartSubscription = async () => {
    if (!accessToken) return;
    setBusy(true);
    setError(null);
    setMessage(null);
    try {
      await startTossBillingSubscription(accessToken, { chargeImmediately: true });
      setMessage('월간 구독을 시작했습니다. 즉시 결제도 함께 요청했습니다.');
      await reload();
    } catch (startError) {
      setError(startError instanceof Error ? startError.message : '구독 시작에 실패했습니다.');
    } finally {
      setBusy(false);
    }
  };

  const handleCancelSubscription = async () => {
    if (!accessToken) return;
    if (!window.confirm('해지를 예약할까요? 이번 이용 기간이 끝날 때까지 Glow Pass 혜택은 유지됩니다.')) return;
    setBusy(true);
    setError(null);
    setMessage(null);
    try {
      await cancelTossBillingSubscription(accessToken);
      setMessage('해지를 예약했습니다. 이용 기간이 끝나면 자동으로 종료됩니다.');
      await reload();
    } catch (cancelError) {
      setError(cancelError instanceof Error ? cancelError.message : '해지 예약에 실패했습니다.');
    } finally {
      setBusy(false);
    }
  };

  const handleWithdrawCancel = async () => {
    if (!accessToken) return;
    setBusy(true);
    setError(null);
    setMessage(null);
    try {
      await withdrawTossBillingCancel(accessToken);
      setMessage('해지 예약을 철회했습니다.');
      await reload();
    } catch (withdrawError) {
      setError(withdrawError instanceof Error ? withdrawError.message : '해지 철회에 실패했습니다.');
    } finally {
      setBusy(false);
    }
  };

  const handleChargeNow = async () => {
    if (!accessToken) return;
    setBusy(true);
    setError(null);
    setMessage(null);
    try {
      await chargeTossBillingNow(accessToken);
      setMessage('테스트 자동결제를 요청했습니다.');
      await reload();
    } catch (chargeError) {
      setError(chargeError instanceof Error ? chargeError.message : '즉시 결제에 실패했습니다.');
    } finally {
      setBusy(false);
    }
  };

  const handleChangeDefault = async (methodId: number) => {
    if (!accessToken) return;
    setBusy(true);
    setError(null);
    setMessage(null);
    try {
      await setTossBillingDefaultMethod(accessToken, methodId);
      setSelectedMethodId(methodId);
      const unpaidCharges = (overview?.charges ?? []).filter(charge => charge.status === 'FAILED');
      if (unpaidCharges.length > 0) {
        const payUnpaid = window.confirm(
          '기본 카드를 변경했습니다. 미납된 정기결제도 이 카드로 지금 결제할까요?',
        );
        if (payUnpaid) {
          for (const charge of unpaidCharges) {
            await retryTossBillingCharge(accessToken, charge.id, {
              methodId,
              setAsDefault: false,
            });
          }
          setMessage('기본 카드를 바꾸고 미납 회차도 결제했습니다. 다음 정기결제부터 새 카드가 사용됩니다.');
        } else {
          setMessage('기본 카드를 변경했습니다. 다음 정기결제부터 새 카드로 청구됩니다.');
        }
      } else {
        setMessage('기본 카드를 변경했습니다. 다음 정기결제부터 새 카드로 청구됩니다.');
      }
      await reload();
    } catch (defaultError) {
      setError(defaultError instanceof Error ? defaultError.message : '기본 카드 변경에 실패했습니다.');
    } finally {
      setBusy(false);
    }
  };

  const handleDeactivate = async (methodId: number) => {
    if (!accessToken) return;
    if (!window.confirm('이 카드를 삭제할까요?')) return;
    setBusy(true);
    setError(null);
    setMessage(null);
    try {
      await deactivateTossBillingMethod(accessToken, methodId);
      setMessage('카드를 삭제했습니다.');
      await reload();
    } catch (deactivateError) {
      setError(deactivateError instanceof Error ? deactivateError.message : '카드 삭제에 실패했습니다.');
    } finally {
      setBusy(false);
    }
  };

  const handleRetry = async (chargeId: number, setAsDefault: boolean) => {
    if (!accessToken || !selectedMethodId) return;
    setBusy(true);
    setError(null);
    setMessage(null);
    try {
      await retryTossBillingCharge(accessToken, chargeId, {
        methodId: selectedMethodId,
        setAsDefault,
      });
      setMessage(
        setAsDefault
          ? '미납 회차를 결제하고 기본 카드를 변경했습니다.'
          : '미납 회차를 선택한 카드로 결제했습니다.',
      );
      await reload();
    } catch (retryError) {
      setError(retryError instanceof Error ? retryError.message : '재결제에 실패했습니다.');
    } finally {
      setBusy(false);
    }
  };

  if (!authReady) {
    return (
      <main className={styles.page}>
        <div className={styles.container}>
          <p className={styles.status}>로그인 상태를 확인하는 중입니다.</p>
        </div>
      </main>
    );
  }

  if (!accessToken || !user) {
    return (
      <main className={styles.page}>
        <div className={styles.container}>
          <h1 className={styles.title}>자동결제(빌링)</h1>
          <p className={styles.lead}>로그인 후 카드 등록과 월간 구독을 이용할 수 있습니다.</p>
          <Link to="/" className={styles.secondaryButton}>
            홈으로
          </Link>
        </div>
      </main>
    );
  }

  const methods = overview?.methods ?? [];
  const unpaid = overview?.charges.some(charge => charge.status === 'FAILED') ?? false;

  return (
    <main className={styles.page}>
      <div className={styles.container}>
        <header>
          <h1 className={styles.title}>Glow Pass 정기결제</h1>
          <p className={styles.lead}>
            카드는 실패와 관계없이 언제든 바꿀 수 있습니다. 결제 실패가 있을 때만 미납 회차를 다른
            카드로 다시 결제하면 됩니다.
          </p>
        </header>

        {loading ? <p className={styles.status}>불러오는 중...</p> : null}
        {error ? <p className={styles.error}>{error}</p> : null}
        {message ? <p className={styles.success}>{message}</p> : null}
        {unpaid ? (
          <p className={styles.error}>
            미납된 정기결제가 있습니다. 기본 카드를 바꾸거나, 아래에서 카드를 골라 미납만 결제할 수
            있습니다.
          </p>
        ) : null}

        <BillingMethodCard
          methods={methods}
          selectedMethodId={selectedMethodId}
          hasUnpaid={unpaid}
          busy={busy}
          onSelect={setSelectedMethodId}
          onRegister={setAsDefault => {
            void handleRegisterCard(setAsDefault);
          }}
          onChangeDefault={methodId => {
            void handleChangeDefault(methodId);
          }}
          onDeactivate={methodId => {
            void handleDeactivate(methodId);
          }}
        />
        <BillingSubscriptionCard
          subscription={overview?.subscription ?? null}
          hasMethod={methods.length > 0}
          busy={busy}
          onStart={() => {
            void handleStartSubscription();
          }}
          onCancel={() => {
            void handleCancelSubscription();
          }}
          onWithdrawCancel={() => {
            void handleWithdrawCancel();
          }}
          onChargeNow={() => {
            void handleChargeNow();
          }}
        />
        <BillingChargeList
          charges={overview?.charges ?? []}
          methods={methods}
          selectedMethodId={selectedMethodId}
          busy={busy}
          onRetryThisCycle={chargeId => {
            void handleRetry(chargeId, false);
          }}
          onRetryAndSetDefault={chargeId => {
            void handleRetry(chargeId, true);
          }}
        />

        <div className={styles.rowActions}>
          <Link to="/my-payments" className={styles.secondaryButton}>
            일반 결제 내역
          </Link>
          <Link to="/mypage" className={styles.secondaryButton}>
            마이페이지
          </Link>
        </div>
      </div>
    </main>
  );
}

export default BillingPage;
