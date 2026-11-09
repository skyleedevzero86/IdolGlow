import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import {
  fetchTossBillingOverview,
  issueTossBillingKey,
  startTossBillingSubscription,
} from '../../../shared/data/tossBilling/tossBillingApi';
import styles from './BillingPage.module.css';

const SET_AS_DEFAULT_KEY = 'tossBilling.setAsDefault';

export function BillingSuccessPage() {
  const { accessToken, authReady } = useAuth();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);

  useEffect(() => {
    if (!authReady) return;
    if (!accessToken) {
      setError('로그인이 필요합니다.');
      return;
    }

    const customerKey = searchParams.get('customerKey')?.trim() ?? '';
    const authKey = searchParams.get('authKey')?.trim() ?? '';
    if (!customerKey || !authKey) {
      setError('카드 인증 정보가 없습니다. customerKey/authKey를 확인하세요.');
      return;
    }

    let cancelled = false;
    void (async () => {
      try {
        const setAsDefault = sessionStorage.getItem(SET_AS_DEFAULT_KEY) !== 'false';
        sessionStorage.removeItem(SET_AS_DEFAULT_KEY);
        await issueTossBillingKey(accessToken, { authKey, customerKey, setAsDefault });
        const overview = await fetchTossBillingOverview(accessToken);
        if (!overview.subscription) {
          await startTossBillingSubscription(accessToken, { chargeImmediately: true });
        }
        if (!cancelled) {
          setDone(true);
          window.setTimeout(() => navigate('/billing', { replace: true }), 1200);
        }
      } catch (issueError) {
        if (!cancelled) {
          setError(issueError instanceof Error ? issueError.message : '빌링키 발급에 실패했습니다.');
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [accessToken, authReady, navigate, searchParams]);

  return (
    <main className={styles.page}>
      <div className={styles.container}>
        <h1 className={styles.title}>카드 등록 성공</h1>
        {done ? <p className={styles.success}>카드를 등록했습니다. 빌링 페이지로 이동합니다.</p> : null}
        {error ? <p className={styles.error}>{error}</p> : null}
        {!done && !error ? <p className={styles.status}>빌링키를 발급하는 중입니다...</p> : null}
        <div className={styles.rowActions}>
          <Link to="/billing" className={styles.primaryButton}>
            빌링 페이지로
          </Link>
        </div>
      </div>
    </main>
  );
}

export default BillingSuccessPage;
