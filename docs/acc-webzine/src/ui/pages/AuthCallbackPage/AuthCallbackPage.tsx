import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { reissueAccessToken } from '../../../auth/authApi';
import { useAuth } from '../../../auth/AuthContext';
import { LoadingSpinner } from '../../components/LoadingSpinner/LoadingSpinner';
import styles from './AuthCallbackPage.module.css';

export function AuthCallbackPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { applyAccessToken } = useAuth();

  useEffect(() => {
    let cancelled = false;

    void (async () => {
      if (searchParams.get('oauth_error') === '1') {
        navigate('/', { replace: true });
        return;
      }

      const token = await reissueAccessToken();
      if (cancelled) {
        return;
      }

      if (token) {
        await applyAccessToken(token);
        navigate('/mypage', { replace: true });
        return;
      }

      navigate('/', { replace: true });
    })();

    return () => {
      cancelled = true;
    };
  }, [applyAccessToken, navigate, searchParams]);

  return (
    <div className={styles.root}>
      <LoadingSpinner />
      <p className={styles.message}>로그인 처리 중…</p>
    </div>
  );
}
