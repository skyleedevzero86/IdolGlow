import {
  type ReactNode,
  useCallback,
  useEffect,
  useRef,
  useState,
} from "react";
import { Link, useLocation } from "react-router-dom";
import { useAuth } from "../../../auth/AuthContext";
import { ApiError } from "../../../shared/data/apiClient";
import { setContentLiked } from "../../../shared/data/contentLikeApi";
import {
  GLOW_FEED_CATEGORY_LABELS,
  type GlowFeedCategory,
  type GlowFeedHighlightSort,
  type GlowFeedItem,
  fetchGlowFeed,
  fetchGlowFeedHighlights,
} from "../../../shared/data/glowFeedApi";
import { shareLink } from "../../../shared/data/shareLink";
import styles from "./GlowAllFeed.module.css";
import { GlowCommunityLoginPrompt } from "./GlowCommunityLoginPrompt";
import pageStyles from "./GlowPage.module.css";

const PAGE_SIZE = 12;
const TOAST_MS = 2000;

const CATEGORY_CLASS: Record<GlowFeedCategory, string> = {
  trend: "categoryTrend",
  news: "categoryNews",
  recent: "categoryRecent",
  local: "categoryLocal",
};

const errorMessage = (error: unknown, fallback: string): string =>
  error instanceof ApiError || error instanceof Error
    ? error.message || fallback
    : fallback;

const likeKey = (item: GlowFeedItem): string =>
  `${item.likeTargetType}:${item.likeTargetKey}`;

const HeartIcon = () => (
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    aria-hidden="true"
  >
    <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
  </svg>
);

const ShareIcon = () => (
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    aria-hidden="true"
  >
    <circle cx="18" cy="5" r="3" />
    <circle cx="6" cy="12" r="3" />
    <circle cx="18" cy="19" r="3" />
    <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" />
    <line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
  </svg>
);

const CardImage = ({
  item,
  eager,
}: { readonly item: GlowFeedItem; readonly eager: boolean }) =>
  item.imageUrl ? (
    <img src={item.imageUrl} alt="" loading={eager ? "eager" : "lazy"} />
  ) : (
    <span
      className={`${styles.placeholder} ${styles[item.category]}`}
      aria-hidden="true"
    >
      {GLOW_FEED_CATEGORY_LABELS[item.category]}
    </span>
  );

const CategoryBadge = ({
  category,
}: { readonly category: GlowFeedCategory }) => (
  <span
    className={`${pageStyles.categoryBadge} ${pageStyles[CATEGORY_CLASS[category]]}`}
  >
    {GLOW_FEED_CATEGORY_LABELS[category]}
  </span>
);

const footerLabel = (item: GlowFeedItem): string | null =>
  item.volume !== null ? `Vol. ${item.volume}` : item.meta;

export const GlowAllFeed = ({
  filterBar,
}: { readonly filterBar: ReactNode }) => {
  const location = useLocation();
  const { accessToken, authReady, user } = useAuth();
  const token = user ? accessToken : null;
  const [items, setItems] = useState<readonly GlowFeedItem[]>([]);
  const [page, setPage] = useState(0);
  const [hasNext, setHasNext] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [popularTab, setPopularTab] = useState<GlowFeedHighlightSort>("latest");
  const [highlights, setHighlights] = useState<readonly GlowFeedItem[]>([]);
  const [highlightLoading, setHighlightLoading] = useState(true);
  const [highlightError, setHighlightError] = useState<string | null>(null);
  const [pendingLikes, setPendingLikes] = useState<ReadonlySet<string>>(
    new Set(),
  );
  const [loginOpen, setLoginOpen] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const tokenRef = useRef(token);
  tokenRef.current = token;

  // biome-ignore lint/correctness/useExhaustiveDependencies: reloadKey is an intentional re-run trigger
  useEffect(() => {
    if (!authReady) return;
    const controller = new AbortController();
    setLoading(true);
    setError(null);
    fetchGlowFeed(0, PAGE_SIZE, token, controller.signal)
      .then((result) => {
        setItems(result.items);
        setPage(0);
        setHasNext(result.hasNext);
      })
      .catch((loadError: unknown) => {
        if (controller.signal.aborted) return;
        setItems([]);
        setHasNext(false);
        setError(errorMessage(loadError, "콘텐츠를 불러오지 못했습니다."));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [authReady, token, reloadKey]);

  useEffect(() => {
    if (!authReady) return;
    const controller = new AbortController();
    setHighlightLoading(true);
    setHighlightError(null);
    fetchGlowFeedHighlights(popularTab, token, controller.signal)
      .then(setHighlights)
      .catch((loadError: unknown) => {
        if (controller.signal.aborted) return;
        setHighlights([]);
        setHighlightError(
          errorMessage(loadError, "추천 콘텐츠를 불러오지 못했습니다."),
        );
      })
      .finally(() => {
        if (!controller.signal.aborted) setHighlightLoading(false);
      });
    return () => controller.abort();
  }, [authReady, token, popularTab]);

  useEffect(() => {
    if (!toast) return;
    const timer = window.setTimeout(() => setToast(null), TOAST_MS);
    return () => window.clearTimeout(timer);
  }, [toast]);

  const handleLoadMore = async () => {
    if (loadingMore || !hasNext) return;
    setLoadingMore(true);
    try {
      const next = await fetchGlowFeed(page + 1, PAGE_SIZE, tokenRef.current);
      setItems((current) => {
        const seen = new Set(current.map((item) => item.id));
        return [...current, ...next.items.filter((item) => !seen.has(item.id))];
      });
      setPage(next.page);
      setHasNext(next.hasNext);
    } catch (loadError) {
      setToast(errorMessage(loadError, "콘텐츠를 더 불러오지 못했습니다."));
    } finally {
      setLoadingMore(false);
    }
  };

  const applyLike = useCallback(
    (key: string, liked: boolean, likeCount: number) => {
      const update = (list: readonly GlowFeedItem[]) =>
        list.map((item) =>
          likeKey(item) === key ? { ...item, liked, likeCount } : item,
        );
      setItems(update);
      setHighlights(update);
    },
    [],
  );

  const handleLike = async (item: GlowFeedItem) => {
    if (!authReady) return;
    const currentToken = tokenRef.current;
    if (!currentToken) {
      setLoginOpen(true);
      return;
    }
    const key = likeKey(item);
    if (pendingLikes.has(key)) return;
    const nextLiked = !item.liked;
    setPendingLikes((current) => new Set(current).add(key));
    applyLike(
      key,
      nextLiked,
      Math.max(0, item.likeCount + (nextLiked ? 1 : -1)),
    );
    try {
      const result = await setContentLiked(
        item.likeTargetType,
        item.likeTargetKey,
        nextLiked,
        currentToken,
      );
      applyLike(key, result.liked, result.likeCount);
    } catch (likeError) {
      applyLike(key, item.liked, item.likeCount);
      if (likeError instanceof ApiError && likeError.status === 401)
        setLoginOpen(true);
      else setToast(errorMessage(likeError, "좋아요를 반영하지 못했습니다."));
    } finally {
      setPendingLikes((current) => {
        const next = new Set(current);
        next.delete(key);
        return next;
      });
    }
  };

  const handleShare = async (item: GlowFeedItem) => {
    const result = await shareLink(item.title, item.path);
    if (result === "copied") setToast("링크를 복사했어요");
    if (result === "failed") setToast("링크를 복사하지 못했어요");
  };

  return (
    <>
      <div className={pageStyles.contentLayout}>
        <div className={pageStyles.mainColumn}>
          {filterBar}

          <div className={pageStyles.articleGrid} aria-busy={loading}>
            {loading ? (
              <p className={styles.status}>콘텐츠를 불러오는 중입니다.</p>
            ) : null}
            {!loading && error ? (
              <p className={`${styles.status} ${styles.error}`} role="alert">
                {error}
                <button
                  type="button"
                  className={styles.retryButton}
                  onClick={() => setReloadKey((key) => key + 1)}
                >
                  다시 시도
                </button>
              </p>
            ) : null}
            {!loading && !error && items.length === 0 ? (
              <p className={styles.status}>아직 등록된 콘텐츠가 없습니다.</p>
            ) : null}
            {!loading
              ? items.map((item, index) => {
                  const footer = footerLabel(item);
                  return (
                    <article className={pageStyles.articleCard} key={item.id}>
                      <Link
                        to={item.path}
                        className={pageStyles.cardImageLink}
                        tabIndex={-1}
                        aria-hidden="true"
                      >
                        <CardImage item={item} eager={index < 6} />
                      </Link>
                      <div className={pageStyles.cardBody}>
                        <div className={pageStyles.cardMeta}>
                          <CategoryBadge category={item.category} />
                          <div className={pageStyles.actions}>
                            <button
                              type="button"
                              className={`${pageStyles.actionButton} ${pageStyles.likeButton} ${item.liked ? pageStyles.liked : ""}`}
                              aria-label={item.liked ? "좋아요 취소" : "좋아요"}
                              aria-pressed={item.liked}
                              disabled={pendingLikes.has(likeKey(item))}
                              onClick={() => void handleLike(item)}
                            >
                              <HeartIcon />
                              <span
                                className={pageStyles.likeCount}
                                aria-hidden="true"
                              >
                                {item.likeCount}
                              </span>
                            </button>
                            <button
                              type="button"
                              className={pageStyles.actionButton}
                              aria-label="공유하기"
                              onClick={() => void handleShare(item)}
                            >
                              <ShareIcon />
                            </button>
                          </div>
                        </div>
                        <h2>
                          <Link to={item.path}>{item.title}</Link>
                        </h2>
                        <div className={pageStyles.cardFooter}>
                          {footer ? <strong>{footer}</strong> : null}
                          <span>
                            {item.tags
                              .slice(0, 2)
                              .map((tag) => `#${tag}`)
                              .join(" ")}
                          </span>
                        </div>
                      </div>
                    </article>
                  );
                })
              : null}
          </div>
        </div>

        <aside
          className={pageStyles.popularPanel}
          aria-label="추천 콘텐츠 정렬"
        >
          <div
            className={pageStyles.popularHeader}
            role="tablist"
            aria-label="추천 콘텐츠 정렬"
          >
            <button
              type="button"
              role="tab"
              aria-selected={popularTab === "latest"}
              className={
                popularTab === "latest"
                  ? pageStyles.activePopularTab
                  : undefined
              }
              onClick={() => setPopularTab("latest")}
            >
              최신 콘텐츠
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={popularTab === "popular"}
              className={
                popularTab === "popular"
                  ? pageStyles.activePopularTab
                  : undefined
              }
              onClick={() => setPopularTab("popular")}
            >
              많이 본 콘텐츠
            </button>
          </div>
          <div className={pageStyles.popularList}>
            {highlightLoading ? (
              <p className={styles.sideStatus}>불러오는 중입니다.</p>
            ) : null}
            {!highlightLoading && highlightError ? (
              <p className={styles.sideStatus} role="alert">
                {highlightError}
              </p>
            ) : null}
            {!highlightLoading && !highlightError && highlights.length === 0 ? (
              <p className={styles.sideStatus}>표시할 콘텐츠가 없습니다.</p>
            ) : null}
            {!highlightLoading
              ? highlights.map((item) => (
                  <article
                    className={pageStyles.popularCard}
                    key={`${popularTab}-${item.id}`}
                  >
                    <Link to={item.path} tabIndex={-1} aria-hidden="true">
                      <CardImage item={item} eager={false} />
                    </Link>
                    <div>
                      <div className={pageStyles.popularMeta}>
                        <CategoryBadge category={item.category} />
                        <span>
                          ♡ {item.likeCount}　◉ {item.viewerCount}
                        </span>
                      </div>
                      <h3>
                        <Link to={item.path}>{item.title}</Link>
                      </h3>
                      {footerLabel(item) ? <p>{footerLabel(item)}</p> : null}
                    </div>
                  </article>
                ))
              : null}
          </div>
        </aside>
      </div>
      {hasNext && !loading ? (
        <button
          type="button"
          className={pageStyles.moreButton}
          aria-label={loadingMore ? "콘텐츠 불러오는 중" : "콘텐츠 더 보기"}
          aria-busy={loadingMore}
          disabled={loadingMore}
          onClick={() => void handleLoadMore()}
        >
          {loadingMore ? (
            <span className={pageStyles.loadingSpinner} aria-hidden="true" />
          ) : (
            "+"
          )}
        </button>
      ) : null}
      {loginOpen ? (
        <GlowCommunityLoginPrompt
          message="좋아요를 누르려면 로그인해 주세요."
          redirectTo={`${location.pathname}${location.search}${location.hash}`}
          onClose={() => setLoginOpen(false)}
        />
      ) : null}
      {toast ? (
        <output className={styles.toast} aria-live="polite">
          {toast}
        </output>
      ) : null}
    </>
  );
};

export default GlowAllFeed;
