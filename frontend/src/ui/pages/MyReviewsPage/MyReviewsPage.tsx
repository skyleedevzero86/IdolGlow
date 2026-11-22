import { useCallback, useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { useAuth } from "../../../auth/AuthContext";
import {
  type MyProductReviewSummary,
  fetchMyReviews,
} from "../../../auth/authApi";
import {
  type OffsetPage,
  apiErrorMessage,
} from "../../../shared/data/apiClient";
import {
  type MyReview,
  contentTargetTypeLabel,
  fetchMyContentReviews,
} from "../../../shared/data/contentCommentApi";
import {
  type ContentView,
  clearContentViews,
  deleteContentView,
  fetchMyContentViews,
} from "../../../shared/data/contentViewApi";
import {
  type GlowCommunityPost,
  fetchMyGlowCommunityPosts,
  glowCommunityPostPath,
  glowCommunityTagLabel,
} from "../../../shared/data/glowCommunityApi";
import { formatIdolGlowDateTime } from "../../../shared/format/idolGlowDateTime";
import { markdownToPlainText } from "../../../shared/markdown/safeMarkdown";
import { SimplePager } from "../../components/SimplePager/SimplePager";
import shellStyles from "../AdminMarketingPage/AdminMarketingPage.module.css";
import { AdminMarketingShell } from "../AdminMarketingPage/AdminMarketingShell";
import plainStyles from "../MyPagePage/MyPagePlain.module.css";
import styles from "./MyReviewsPage.module.css";

const PAGE_SIZE = 10;

type TabId = "views" | "reviews" | "posts";

const TABS: readonly { readonly id: TabId; readonly label: string }[] = [
  { id: "views", label: "최근 본 콘텐츠" },
  { id: "reviews", label: "내가 남긴 리뷰" },
  { id: "posts", label: "내 게시글" },
];

const parseTab = (raw: string | null): TabId =>
  raw === "reviews" || raw === "posts" ? raw : "views";

function ratingStars(rating: number): string {
  const value = Math.max(0, Math.min(5, Math.round(rating)));
  return `${"★".repeat(value)}${"☆".repeat(5 - value)}`;
}

const reviewLink = (review: MyReview): string | null => {
  const path = review.targetPath?.trim();
  if (!path || !path.startsWith("/") || path.startsWith("//")) return null;
  return `${path.split("#")[0]}#comment-${review.commentId}`;
};

function usePagedResource<T>(
  enabled: boolean,
  loader: (page: number) => Promise<OffsetPage<T>>,
  fallbackMessage: string,
) {
  const [page, setPage] = useState(0);
  const [data, setData] = useState<OffsetPage<T> | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  // biome-ignore lint/correctness/useExhaustiveDependencies: dependency is an intentional re-run trigger
  useEffect(() => {
    if (!enabled) return;
    let cancelled = false;
    setLoading(true);
    setError(null);
    loader(page)
      .then((response) => {
        if (cancelled) return;
        if (response.items.length === 0 && page > 0) {
          setPage(Math.max(0, Math.min(page - 1, response.totalPages - 1)));
          return;
        }
        setData(response);
      })
      .catch((loadError: unknown) => {
        if (!cancelled) setError(apiErrorMessage(loadError, fallbackMessage));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [enabled, fallbackMessage, loader, page, reloadKey]);

  return {
    page,
    setPage,
    data,
    loading,
    error,
    reload: () => setReloadKey((value) => value + 1),
  };
}

export function MyReviewsPage() {
  const { accessToken, authReady, user } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const tab = parseTab(searchParams.get("tab"));
  const ready = Boolean(authReady && accessToken && user);
  const [actionError, setActionError] = useState<string | null>(null);
  const [productReviews, setProductReviews] = useState<
    readonly MyProductReviewSummary[]
  >([]);
  const [productLoading, setProductLoading] = useState(false);
  const [productError, setProductError] = useState<string | null>(null);

  const loadViews = useCallback(
    (page: number) => fetchMyContentViews(accessToken ?? "", page, PAGE_SIZE),
    [accessToken],
  );
  const loadReviews = useCallback(
    (page: number) => fetchMyContentReviews(accessToken ?? "", page, PAGE_SIZE),
    [accessToken],
  );
  const loadPosts = useCallback(
    (page: number) =>
      fetchMyGlowCommunityPosts(accessToken ?? "", page, PAGE_SIZE),
    [accessToken],
  );

  const views = usePagedResource<ContentView>(
    ready && tab === "views",
    loadViews,
    "최근 본 콘텐츠를 불러오지 못했습니다.",
  );
  const reviews = usePagedResource<MyReview>(
    ready && tab === "reviews",
    loadReviews,
    "내가 남긴 리뷰를 불러오지 못했습니다.",
  );
  const posts = usePagedResource<GlowCommunityPost>(
    ready && tab === "posts",
    loadPosts,
    "내 게시글을 불러오지 못했습니다.",
  );

  useEffect(() => {
    if (!ready || !accessToken || tab !== "reviews") return;
    let cancelled = false;
    setProductLoading(true);
    setProductError(null);
    fetchMyReviews(accessToken)
      .then((response) => {
        if (cancelled) return;
        if (response == null) {
          setProductError("상품 리뷰를 불러오지 못했습니다.");
          return;
        }
        setProductReviews(response);
      })
      .finally(() => {
        if (!cancelled) setProductLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [accessToken, ready, tab]);

  const selectTab = (next: TabId) => {
    setActionError(null);
    const params = new URLSearchParams(searchParams);
    if (next === "views") params.delete("tab");
    else params.set("tab", next);
    setSearchParams(params, { replace: true });
  };

  const removeView = async (view: ContentView) => {
    if (!accessToken) return;
    if (!window.confirm(`「${view.title}」 기록을 삭제할까요?`)) return;
    try {
      await deleteContentView(view.id, accessToken);
      views.reload();
    } catch (error) {
      setActionError(apiErrorMessage(error, "기록을 삭제하지 못했습니다."));
    }
  };

  const removeAllViews = async () => {
    if (!accessToken) return;
    if (!window.confirm("최근 본 콘텐츠 기록을 모두 삭제할까요?")) return;
    try {
      await clearContentViews(accessToken);
      views.setPage(0);
      views.reload();
    } catch (error) {
      setActionError(apiErrorMessage(error, "기록을 삭제하지 못했습니다."));
    }
  };

  if (!authReady) {
    return (
      <main className={shellStyles.page}>
        <div className={shellStyles.loading}>정보를 불러오는 중입니다.</div>
      </main>
    );
  }

  const countLabel = (resource: {
    data: OffsetPage<unknown> | null;
    loading: boolean;
  }) =>
    resource.loading && !resource.data
      ? "…"
      : resource.data
        ? resource.data.totalElements.toLocaleString()
        : "-";

  return (
    <AdminMarketingShell
      currentPath="/myreviewsfh"
      title="나의 리뷰"
      description="최근 본 콘텐츠, 내가 남긴 댓글·리뷰, 지역사회 게시글을 한곳에서 확인합니다."
      statusText={user ? `${user.nickname || user.email}님의 활동` : null}
      classNames={{ toolbarCard: plainStyles.flatToolbar }}
      stats={[
        { label: "최근 본 콘텐츠", value: countLabel(views) },
        { label: "내가 남긴 리뷰", value: countLabel(reviews) },
        { label: "내 게시글", value: countLabel(posts) },
      ]}
    >
      <section className={styles.panel}>
        <div className={styles.tabs} role="tablist" aria-label="나의 리뷰 메뉴">
          {TABS.map((item) => (
            <button
              key={item.id}
              type="button"
              role="tab"
              aria-selected={tab === item.id}
              className={tab === item.id ? styles.tabActive : styles.tab}
              onClick={() => selectTab(item.id)}
            >
              {item.label}
            </button>
          ))}
        </div>

        {actionError ? (
          <p className={styles.errorBox} role="alert">
            {actionError}
          </p>
        ) : null}

        {tab === "views" ? (
          <div className={styles.tabBody}>
            <div className={styles.sectionHead}>
              <h2>최근 본 콘텐츠</h2>
              {views.data && views.data.totalElements > 0 ? (
                <button
                  type="button"
                  className={styles.ghostButton}
                  onClick={() => void removeAllViews()}
                >
                  전체 삭제
                </button>
              ) : null}
            </div>
            {views.error ? (
              <p className={styles.errorBox}>{views.error}</p>
            ) : null}
            {views.loading && !views.data ? (
              <p className={styles.status}>불러오는 중입니다.</p>
            ) : null}
            {views.data && views.data.items.length === 0 ? (
              <p className={styles.status}>아직 본 콘텐츠가 없습니다.</p>
            ) : null}
            {views.data && views.data.items.length > 0 ? (
              <ul className={styles.rows}>
                {views.data.items.map((view) => (
                  <li key={view.id} className={styles.row}>
                    <div className={styles.rowMain}>
                      <span className={styles.typeBadge}>
                        {contentTargetTypeLabel(view.contentType)}
                      </span>
                      <Link to={view.path} className={styles.rowTitle}>
                        {view.title}
                      </Link>
                      <time className={styles.rowDate}>
                        {formatIdolGlowDateTime(view.viewedAt)}
                      </time>
                    </div>
                    <button
                      type="button"
                      className={styles.iconButton}
                      aria-label={`${view.title} 기록 삭제`}
                      onClick={() => void removeView(view)}
                    >
                      ×
                    </button>
                  </li>
                ))}
              </ul>
            ) : null}
            <SimplePager
              page={views.page}
              totalPages={views.data?.totalPages ?? 1}
              onChange={views.setPage}
              label="최근 본 콘텐츠 페이지"
            />
          </div>
        ) : null}

        {tab === "reviews" ? (
          <div className={styles.tabBody}>
            <div className={styles.sectionHead}>
              <h2>댓글·답글</h2>
            </div>
            {reviews.error ? (
              <p className={styles.errorBox}>{reviews.error}</p>
            ) : null}
            {reviews.loading && !reviews.data ? (
              <p className={styles.status}>불러오는 중입니다.</p>
            ) : null}
            {reviews.data && reviews.data.items.length === 0 ? (
              <p className={styles.status}>아직 남긴 댓글이 없습니다.</p>
            ) : null}
            {reviews.data && reviews.data.items.length > 0 ? (
              <ul className={styles.rows}>
                {reviews.data.items.map((review) => {
                  const link = reviewLink(review);
                  return (
                    <li key={review.commentId} className={styles.reviewRow}>
                      <div className={styles.rowMain}>
                        <span className={styles.typeBadge}>
                          {contentTargetTypeLabel(review.targetType)}
                        </span>
                        {link ? (
                          <Link to={link} className={styles.rowTitle}>
                            {review.targetTitle}
                          </Link>
                        ) : (
                          <span className={styles.rowTitle}>
                            {review.targetTitle}
                          </span>
                        )}
                        <time className={styles.rowDate}>
                          {formatIdolGlowDateTime(review.createdAt)}
                        </time>
                      </div>
                      {review.parentId && review.parentExcerpt ? (
                        <p className={styles.parentExcerpt}>
                          ↳ {review.parentExcerpt}
                        </p>
                      ) : null}
                      <p className={styles.excerpt}>
                        {review.excerpt ||
                          markdownToPlainText(review.contentMarkdown)}
                      </p>
                    </li>
                  );
                })}
              </ul>
            ) : null}
            <SimplePager
              page={reviews.page}
              totalPages={reviews.data?.totalPages ?? 1}
              onChange={reviews.setPage}
              label="내가 남긴 리뷰 페이지"
            />

            <div className={styles.sectionHead}>
              <h2>상품 리뷰 · 방문 인증</h2>
            </div>
            {productError ? (
              <p className={styles.errorBox}>{productError}</p>
            ) : null}
            {productLoading ? (
              <p className={styles.status}>상품 리뷰를 불러오는 중입니다.</p>
            ) : null}
            {!productLoading && !productError && productReviews.length === 0 ? (
              <p className={styles.status}>아직 작성한 상품 리뷰가 없습니다.</p>
            ) : null}
            {!productLoading && productReviews.length > 0 ? (
              <ul className={styles.rows}>
                {productReviews.map((review) => (
                  <li key={review.reviewId} className={styles.reviewRow}>
                    <div className={styles.rowMain}>
                      <span className={styles.typeBadge}>상품</span>
                      <Link
                        to={`/products/${review.productId}`}
                        className={styles.rowTitle}
                      >
                        상품 #{review.productId}
                      </Link>
                      <span
                        className={styles.rating}
                        aria-label={`평점 ${review.rating}점`}
                      >
                        {ratingStars(review.rating)}
                      </span>
                      <time className={styles.rowDate}>
                        {formatIdolGlowDateTime(review.createdAt)}
                      </time>
                    </div>
                    <div className={styles.chips}>
                      {review.verifiedPurchase ? (
                        <span className={styles.chip}>방문 인증</span>
                      ) : null}
                      <span
                        className={
                          review.hidden ? styles.chipHidden : styles.chip
                        }
                      >
                        {review.hidden ? "비공개" : "공개"}
                      </span>
                      <span className={styles.chip}>
                        도움 {review.helpfulCount}
                      </span>
                    </div>
                    <p className={styles.excerpt}>{review.content}</p>
                    {review.images.length > 0 ? (
                      <div className={styles.thumbs}>
                        {review.images.map((image) => (
                          <a
                            key={image.id}
                            className={styles.thumb}
                            href={image.url}
                            target="_blank"
                            rel="noreferrer"
                            title={image.originalFilename}
                          >
                            <img src={image.url} alt="" />
                          </a>
                        ))}
                      </div>
                    ) : null}
                  </li>
                ))}
              </ul>
            ) : null}
          </div>
        ) : null}

        {tab === "posts" ? (
          <div className={styles.tabBody}>
            <div className={styles.sectionHead}>
              <h2>지역사회 게시글</h2>
              <Link to="/glow?tab=local" className={styles.ghostButton}>
                지역사회 가기
              </Link>
            </div>
            {posts.error ? (
              <p className={styles.errorBox}>{posts.error}</p>
            ) : null}
            {posts.loading && !posts.data ? (
              <p className={styles.status}>불러오는 중입니다.</p>
            ) : null}
            {posts.data && posts.data.items.length === 0 ? (
              <p className={styles.status}>아직 작성한 게시글이 없습니다.</p>
            ) : null}
            {posts.data && posts.data.items.length > 0 ? (
              <ul className={styles.rows}>
                {posts.data.items.map((post) => (
                  <li
                    key={post.id}
                    className={
                      post.blinded ? styles.blindedRow : styles.reviewRow
                    }
                  >
                    <div className={styles.rowMain}>
                      <span className={styles.typeBadge}>
                        {glowCommunityTagLabel(post)}
                      </span>
                      {post.blinded ? (
                        <span className={styles.blindBadge}>블라인드</span>
                      ) : null}
                      <Link
                        to={glowCommunityPostPath(post.id)}
                        className={styles.rowTitle}
                      >
                        {post.title}
                      </Link>
                      <time className={styles.rowDate}>
                        {formatIdolGlowDateTime(post.createdAt)}
                      </time>
                    </div>
                    <p className={styles.postStats}>
                      좋아요 {post.likeCount.toLocaleString()} · 댓글{" "}
                      {post.commentCount.toLocaleString()}
                      {post.blinded && post.blindReason
                        ? ` · 블라인드 사유: ${post.blindReason}`
                        : ""}
                    </p>
                  </li>
                ))}
              </ul>
            ) : null}
            <SimplePager
              page={posts.page}
              totalPages={posts.data?.totalPages ?? 1}
              onChange={posts.setPage}
              label="내 게시글 페이지"
            />
          </div>
        ) : null}
      </section>
    </AdminMarketingShell>
  );
}

export default MyReviewsPage;
