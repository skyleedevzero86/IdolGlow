import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../../auth/AuthContext";
import {
  fetchMyReviews,
  type MyProductReviewSummary,
} from "../../../auth/authApi";
import shellStyles from "../AdminMarketingPage/AdminMarketingPage.module.css";
import { AdminMarketingShell } from "../AdminMarketingPage/AdminMarketingShell";
import plainStyles from "../MyPagePage/MyPagePlain.module.css";
import styles from "./MyReviewsPage.module.css";

function formatCreatedAt(raw: string): string {
  try {
    const date = new Date(raw);
    if (Number.isNaN(date.getTime())) {
      return raw;
    }
    return date.toLocaleString("ko-KR");
  } catch {
    return raw;
  }
}

function ratingStars(rating: number): string {
  const value = Math.max(0, Math.min(5, Math.round(rating)));
  return `${"★".repeat(value)}${"☆".repeat(5 - value)}`;
}

export function MyReviewsPage() {
  const { accessToken, authReady, user } = useAuth();
  const [reviews, setReviews] = useState<readonly MyProductReviewSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!authReady || !accessToken || !user) {
      return;
    }
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await fetchMyReviews(accessToken);
        if (response == null) {
          throw new Error("리뷰 목록을 불러오지 못했습니다.");
        }
        setReviews(response);
      } catch (loadError) {
        setError(
          loadError instanceof Error
            ? loadError.message
            : "리뷰 목록을 불러오지 못했습니다.",
        );
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, [accessToken, authReady, user]);

  const visibleCount = reviews.filter((review) => !review.hidden).length;
  const hiddenCount = reviews.length - visibleCount;
  const verifiedCount = reviews.filter((review) => review.verifiedPurchase).length;

  if (!authReady) {
    return (
      <main className={shellStyles.page}>
        <div className={shellStyles.loading}>
          리뷰 정보를 불러오는 중입니다.
        </div>
      </main>
    );
  }

  return (
    <AdminMarketingShell
      currentPath="/myreviewsfh"
      title="나의 리뷰"
      description="내가 작성한 상품 리뷰를 확인할 수 있습니다."
      statusText={user ? `${user.nickname || user.email}님이 작성한 리뷰` : null}
      classNames={{
        toolbarCard: plainStyles.flatToolbar,
      }}
      stats={[
        { label: "작성 리뷰", value: loading ? "…" : String(reviews.length) },
        { label: "공개", value: loading ? "…" : String(visibleCount) },
        { label: "방문 인증", value: loading ? "…" : String(verifiedCount) },
      ]}
    >
      <section className={`${shellStyles.panel} ${plainStyles.flatPanel}`}>
        <div className={shellStyles.panelHeader}>
          <h2 className={shellStyles.panelTitle}>작성 리뷰</h2>
        </div>
        <div className={shellStyles.panelBody}>
          {error ? (
            <p className={plainStyles.userPaymentError}>{error}</p>
          ) : null}
          {loading ? (
            <div className={shellStyles.empty}>
              리뷰 목록을 불러오는 중입니다.
            </div>
          ) : reviews.length === 0 ? (
            <div className={shellStyles.empty}>
              아직 작성한 리뷰가 없습니다. 상품을 이용한 뒤 리뷰를 남겨 보세요.
            </div>
          ) : (
            <div className={plainStyles.userPaymentList}>
              {reviews.map((review) => (
                <article
                  key={review.reviewId}
                  className={`${plainStyles.userPaymentCard} ${styles.reviewCard}`}
                >
                  <div className={plainStyles.userPaymentMeta}>
                    <div className={styles.reviewHeader}>
                      <strong>
                        <Link to={`/products/${review.productId}`}>
                          상품 #{review.productId}
                        </Link>
                      </strong>
                      <span className={styles.rating} aria-label={`평점 ${review.rating}점`}>
                        {ratingStars(review.rating)} {review.rating}/5
                      </span>
                    </div>
                    <span className={plainStyles.userPaymentDate}>
                      {formatCreatedAt(review.createdAt)}
                    </span>
                    <div className={styles.chips}>
                      {review.verifiedPurchase ? (
                        <span className={styles.chip}>방문 인증</span>
                      ) : null}
                      {review.hidden ? (
                        <span className={`${styles.chip} ${styles.chipHidden}`}>
                          비공개
                        </span>
                      ) : (
                        <span className={styles.chip}>공개</span>
                      )}
                      <span className={styles.chip}>도움 {review.helpfulCount}</span>
                    </div>
                    <p className={styles.content}>{review.content}</p>
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
                  </div>
                </article>
              ))}
            </div>
          )}
          {!loading && hiddenCount > 0 ? (
            <p className={styles.hiddenHint}>
              비공개 처리된 리뷰 {hiddenCount}건은 다른 사용자에게는 보이지 않습니다.
            </p>
          ) : null}
        </div>
      </section>
    </AdminMarketingShell>
  );
}

export default MyReviewsPage;
