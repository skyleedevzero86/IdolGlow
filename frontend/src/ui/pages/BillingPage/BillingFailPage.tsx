import { Link, useSearchParams } from 'react-router-dom';
import styles from './BillingPage.module.css';

export function BillingFailPage() {
  const [searchParams] = useSearchParams();
  const code = searchParams.get('code') ?? '';
  const message = searchParams.get('message') ?? '카드 등록에 실패했습니다.';

  return (
    <main className={styles.page}>
      <div className={styles.container}>
        <h1 className={styles.title}>카드 등록 실패</h1>
        <p className={styles.error}>{message}</p>
        {code ? <p className={styles.meta}>에러 코드: {code}</p> : null}
        <div className={styles.rowActions}>
          <Link to="/billing" className={styles.primaryButton}>
            다시 시도
          </Link>
          <Link to="/mypage" className={styles.secondaryButton}>
            마이페이지
          </Link>
        </div>
      </div>
    </main>
  );
}

export default BillingFailPage;
