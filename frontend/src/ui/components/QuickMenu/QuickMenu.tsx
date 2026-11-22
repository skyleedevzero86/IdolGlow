import {
  type TouchEvent,
  useCallback,
  useEffect,
  useRef,
  useState,
} from "react";
import { Link, useLocation } from "react-router-dom";
import { useAuth } from "../../../auth/AuthContext";
import { contentTargetTypeLabel } from "../../../shared/data/contentCommentApi";
import { fetchMyContentViews } from "../../../shared/data/contentViewApi";
import {
  RECENT_VIEWS_CHANGED_EVENT,
  RECENT_VIEWS_STORAGE_KEY,
  type RecentViewEntry,
  readRecentViews,
  recentViewTypeLabel,
  resolveRecentViewImage,
  seedRecentViews,
} from "../../../shared/data/recentViewsStore";
import styles from "./QuickMenu.module.css";

const QUICK_MENU_PATHS = new Set([
  "/",
  "/glow",
  "/exchange-rate",
  "/wish",
  "/events",
  "/archive",
  "/notices",
]);

const QUICK_MENU_DETAIL_PATTERNS = [
  /^\/products\/[^/]+$/,
  /^\/articles\/[^/]+$/,
  /^\/event-info\/[^/]+\/[^/]+$/,
  /^\/notices\/[^/]+$/,
  /^\/events\/[^/]+$/,
];

const SWIPE_THRESHOLD = 30;

const entryLabel = (contentType: string): string =>
  recentViewTypeLabel(contentType) ?? contentTargetTypeLabel(contentType);

const RecentThumb = ({ entry }: { readonly entry: RecentViewEntry }) => {
  const [failed, setFailed] = useState(false);
  const src = resolveRecentViewImage(entry.imageUrl);
  if (!src || failed) {
    return (
      <span className={styles.recentPlaceholder} aria-hidden="true">
        {entryLabel(entry.contentType)}
      </span>
    );
  }
  return (
    <img
      className={styles.recentImage}
      src={src}
      alt=""
      loading="lazy"
      onError={() => setFailed(true)}
    />
  );
};

const SlideChevron = ({
  direction,
}: {
  readonly direction: "prev" | "next";
}) => (
  <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path
      d={direction === "prev" ? "M15 6l-6 6 6 6" : "M9 6l6 6-6 6"}
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>
);

const RecentSlider = ({
  entries,
}: {
  readonly entries: readonly RecentViewEntry[];
}) => {
  const [index, setIndex] = useState(0);
  const touchStartX = useRef<number | null>(null);
  const count = entries.length;
  const firstPath = entries[0]?.path ?? "";
  const [trackedFirst, setTrackedFirst] = useState(firstPath);

  if (trackedFirst !== firstPath) {
    setTrackedFirst(firstPath);
    setIndex(0);
  }

  const current = Math.min(index, Math.max(count - 1, 0));
  const goPrev = () => setIndex(current <= 0 ? count - 1 : current - 1);
  const goNext = () => setIndex(current >= count - 1 ? 0 : current + 1);

  const onTouchStart = (event: TouchEvent<HTMLDivElement>) => {
    touchStartX.current = event.touches[0]?.clientX ?? null;
  };
  const onTouchEnd = (event: TouchEvent<HTMLDivElement>) => {
    const start = touchStartX.current;
    touchStartX.current = null;
    const end = event.changedTouches[0]?.clientX;
    if (start === null || end === undefined || count < 2) return;
    const delta = end - start;
    if (Math.abs(delta) < SWIPE_THRESHOLD) return;
    if (delta < 0) goNext();
    else goPrev();
  };

  return (
    <div
      className={styles.slider}
      aria-roledescription="carousel"
      aria-label="최근 본 콘텐츠 목록"
    >
      <div
        className={styles.sliderViewport}
        onTouchStart={onTouchStart}
        onTouchEnd={onTouchEnd}
      >
        <ul
          className={styles.sliderTrack}
          style={{ transform: `translateX(${-current * 100}%)` }}
        >
          {entries.map((entry, slideIndex) => {
            const active = slideIndex === current;
            return (
              <li
                key={entry.path}
                className={styles.slide}
                aria-roledescription="slide"
                aria-label={`${slideIndex + 1} / ${count}`}
                aria-hidden={!active}
              >
                <Link
                  to={entry.path}
                  className={styles.recentItem}
                  title={entry.title}
                  tabIndex={active ? undefined : -1}
                >
                  <span className={styles.recentThumb}>
                    <RecentThumb entry={entry} />
                  </span>
                  <span className={styles.recentTitle}>{entry.title}</span>
                </Link>
              </li>
            );
          })}
        </ul>
      </div>
      <div className={styles.sliderControls}>
        <button
          type="button"
          className={styles.sliderButton}
          onClick={goPrev}
          disabled={count < 2}
          aria-label="이전 콘텐츠"
        >
          <SlideChevron direction="prev" />
        </button>
        <span className={styles.sliderIndicator} aria-live="polite">
          {`${current + 1} / ${count}`}
        </span>
        <button
          type="button"
          className={styles.sliderButton}
          onClick={goNext}
          disabled={count < 2}
          aria-label="다음 콘텐츠"
        >
          <SlideChevron direction="next" />
        </button>
      </div>
    </div>
  );
};

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
  const { accessToken, authReady, user } = useAuth();
  const [recentViews, setRecentViews] = useState<readonly RecentViewEntry[]>(
    () => readRecentViews(),
  );

  useEffect(() => {
    const refresh = () => setRecentViews(readRecentViews());
    const onStorage = (event: StorageEvent) => {
      if (event.key === null || event.key === RECENT_VIEWS_STORAGE_KEY) {
        refresh();
      }
    };
    window.addEventListener(RECENT_VIEWS_CHANGED_EVENT, refresh);
    window.addEventListener("storage", onStorage);
    return () => {
      window.removeEventListener(RECENT_VIEWS_CHANGED_EVENT, refresh);
      window.removeEventListener("storage", onStorage);
    };
  }, []);

  useEffect(() => {
    if (!authReady || !accessToken || !user) return;
    if (readRecentViews().length > 0) return;
    let cancelled = false;
    fetchMyContentViews(accessToken, 0, 20)
      .then((response) => {
        if (cancelled) return;
        seedRecentViews(
          response.items.map((item) => ({
            contentType: item.contentType,
            contentKey: item.contentKey,
            title: item.title,
            path: item.path,
            imageUrl: item.imageUrl ?? null,
            viewedAt: item.viewedAt,
          })),
        );
      })
      .catch(() => undefined);
    return () => {
      cancelled = true;
    };
  }, [accessToken, authReady, user]);

  const scrollToTop = useCallback(() => {
    const instant = window.matchMedia(
      "(prefers-reduced-motion: reduce)",
    ).matches;
    window.scrollTo({ top: 0, behavior: instant ? "auto" : "smooth" });
  }, []);

  const onContentDetail = QUICK_MENU_DETAIL_PATTERNS.some((pattern) =>
    pattern.test(location.pathname),
  );
  if (!QUICK_MENU_PATHS.has(location.pathname) && !onContentDetail) {
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
          {recentViews.length > 0 ? (
            <RecentSlider entries={recentViews} />
          ) : (
            <p className={styles.noQuickSlide}>최근 본 콘텐츠가 없습니다.</p>
          )}
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
