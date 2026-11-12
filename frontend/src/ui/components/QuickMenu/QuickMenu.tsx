import { useCallback } from "react";
import { Link, useLocation } from "react-router-dom";
import styles from "./QuickMenu.module.css";

const TopArrowIcon = () => (
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2.2"
    aria-hidden="true"
  >
    <path
      d="M12 19V5M5 12l7-7 7 7"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>
);

export const QuickMenu = () => {
  const location = useLocation();
  const quickMenuPaths = new Set([
    "/",
    "/glow",
    "/exchange-rate",
    "/wish",
    "/events",
    "/archive",
    "/notices",
  ]);

  const scrollToTop = useCallback(() => {
    const instant = window.matchMedia(
      "(prefers-reduced-motion: reduce)",
    ).matches;
    window.scrollTo({ top: 0, behavior: instant ? "auto" : "smooth" });
  }, []);

  const onNoticeDetail = /^\/notices\/[^/]+$/.test(location.pathname);
  if (!quickMenuPaths.has(location.pathname) && !onNoticeDetail) {
    return null;
  }

  return (
    <aside className={styles.quickMenuWrap} aria-label="빠른 메뉴">
      <div className={styles.quickMenu}>
        <section
          className={styles.recentSection}
          aria-labelledby="quick-menu-recent-heading"
        >
          <h2 id="quick-menu-recent-heading" className={styles.quickTit}>
            최근 본 콘텐츠
          </h2>
          <p
            className={
              onNoticeDetail
                ? `${styles.noQuickSlide} ${styles.latestNote}`
                : styles.noQuickSlide
            }
          >
            {onNoticeDetail
              ? "가장 최신 게시글 입니다"
              : "최근 본 콘텐츠가 없습니다."}
          </p>
        </section>

        <nav aria-label="바로가기">
          <ul className={styles.quickLink}>
            <li>
              <Link
                to="/notices"
                className={styles.circleLink}
                aria-label="Idol Glow 소식 페이지로 이동"
              >
                <span className={styles.twoLine} aria-hidden="true">
                  <span>Idol Glow</span>
                  <span>소식</span>
                </span>
              </Link>
            </li>
            <li>
              <button
                type="button"
                className={styles.totop}
                onClick={scrollToTop}
                aria-label="맨 위로"
              >
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
