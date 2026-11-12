import { useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { ArticleListPage } from '../ArticleListPage/ArticleListPage';
import styles from './GlowPage.module.css';

export const GlowPage = () => {
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    if (location.hash !== '#exchange-rate') return;
    navigate('/exchange-rate', { replace: true });
  }, [location.hash, navigate]);

  return (
    <div className={styles.glowPage}>
      <main className={styles.glowMain} id="main-content">
        <ArticleListPage embed />
      </main>
    </div>
  );
};

export default GlowPage;
