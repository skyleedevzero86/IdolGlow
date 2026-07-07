/**
 * 우측 고정 퀵메뉴 (Idol Glow 웹진 레퍼런스)
 * — 최근 본 콘텐츠 영역(빈 상태), 바로가기, 맨 위로
 */

import { useCallback } from 'react';
import { Link, useLocation } from 'react-router-dom';
import styles from './QuickMenu.module.css';

const TopArrowIcon = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" aria-hidden="true">
    <path d="M12 19V5M5 12l7-7 7 7" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

export const QuickMenu = () => {
  const location = useLocation();
  const quickMenuPaths = new Set([
    '/',
    '/glow',
    '/exchange-rate',
    '/wish',
    '/events',
    '/archive',
  ]);

  const scrollToTop = useCallback(() => {
    const instant = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    window.scrollTo({ top: 0, behavior: instant ? 'auto' : 'smooth' });
  }, []);

  const handleVolumeClick = useCallback((e: React.MouseEvent<HTMLAnchorElement>) => {
    if (location.pathname !== '/') return;
    e.preventDefault();
    const el = document.getElementById('volume-section');
    const instant = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    el?.scrollIntoView({ behavior: instant ? 'auto' : 'smooth', block: 'start' });
  }, [location.pathname]);

  if (!quickMenuPaths.has(location.pathname)) {
    return null;
  }

  return (
    <aside className={styles.quickMenuWrap} aria-label="빠른 메뉴">
      <div className={styles.quickMenu}>
        <section className={styles.recentSection} aria-labelledby="quick-menu-recent-heading">
          <h2 id="quick-menu-recent-heading" className={styles.quickTit}>
            최근 본 콘텐츠
          </h2>
          <p className={styles.noQuickSlide} role="status">
            최근 본 콘텐츠가 없습니다.
          </p>
        </section>

        <nav aria-label="바로가기">
          <ul className={styles.quickLink}>
            <li>
              <Link to="/" className={styles.circleLink} aria-label="Idol Glow 소식 — 홈으로 이동">
                <span className={styles.twoLine} aria-hidden="true">
                  <span>Idol Glow</span>
                  <span>소식</span>
                </span>
              </Link>
            </li>
            <li>
              <Link
                to={{ pathname: '/', hash: 'volume-section' }}
                className={styles.circleLink}
                aria-label="호별보기 — 홈의 호별 섹션으로 이동"
                onClick={handleVolumeClick}
              >
                호별보기
              </Link>
            </li>
            <li>
              <button type="button" className={styles.totop} onClick={scrollToTop} aria-label="맨 위로">
                <TopArrowIcon />
              </button>
            </li>
          </ul>
        </nav>
      </div>
    </aside>
  );
};

export default QuickMenu;
