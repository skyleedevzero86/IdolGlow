import { type ReactNode, useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  type GlowTodayPhoto,
  type GlowTodayReview,
  fetchGlowTodayPhotos,
  fetchGlowTodayReviews,
} from '../../../shared/data/glowReviewApi';
import glowStyles from './GlowPage.module.css';
import styles from './GlowRecentReviews.module.css';

const PHOTO_PAGE_SIZE = 24;
const REVIEW_COUNT = 6;

const PROMO_IMAGES = [
  'https://images.unsplash.com/photo-1538485399081-7191377e8241?w=360&h=220&fit=crop',
  'https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=360&h=220&fit=crop',
  'https://images.unsplash.com/photo-1556228578-8c89e6adf883?w=360&h=220&fit=crop',
] as const;

const formatDate = (value: string): string => value.split('-').join('.');

const StarIcon = () => (
  <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
    <path d="m12 2.5 2.94 5.96 6.56.95-4.75 4.63 1.12 6.53L12 17.49l-5.87 3.08 1.12-6.53L2.5 9.41l6.56-.95L12 2.5Z" />
  </svg>
);

const Stars = ({ rating }: { readonly rating: number }) => (
  <span className={styles.stars} role="img" aria-label={`별점 ${rating}점`}>
    {Array.from({ length: 5 }, (_, index) => (
      <span key={index} className={index < Math.round(rating) ? styles.starOn : styles.starOff}>
        <StarIcon />
      </span>
    ))}
  </span>
);

const ReviewCard = ({ review }: { readonly review: GlowTodayReview }) => {
  return (
    <article className={styles.reviewCard}>
      <Link to={review.linkUrl} className={styles.placeRow}>
        <div className={styles.placeText}>
          <p className={styles.region}>{review.regionLabel}</p>
          <h3>{review.placeName}</h3>
          <p className={styles.placeRating}>
            <StarIcon />
            {review.placeRating} <span>({review.placeReviewCount.toLocaleString()})</span>
          </p>
        </div>
        <img src={review.placeImageUrl} alt="" loading="lazy" />
      </Link>

      <div className={styles.reviewBody}>
        <div className={styles.ratingRow}>
          <Stars rating={review.rating} />
          <strong>{review.rating}</strong>
        </div>
        <p className={styles.content}>{review.content}</p>
        <p className={styles.reviewer}>
          <span>{review.reviewerName}</span>
          <time dateTime={review.createdAt}>{formatDate(review.createdAt)}</time>
        </p>
      </div>
    </article>
  );
};

export const GlowRecentReviews = ({ filterBar }: { readonly filterBar: ReactNode }) => {
  const [photos, setPhotos] = useState<readonly GlowTodayPhoto[]>([]);
  const [photoPage, setPhotoPage] = useState(0);
  const [photoHasNext, setPhotoHasNext] = useState(false);
  const [photoLoading, setPhotoLoading] = useState(false);
  const [reviews, setReviews] = useState<readonly GlowTodayReview[]>([]);
  const [reviewLoading, setReviewLoading] = useState(false);
  const [activePhoto, setActivePhoto] = useState<GlowTodayPhoto | null>(null);

  useEffect(() => {
    let cancelled = false;
    setPhotoLoading(true);
    setReviewLoading(true);

    void fetchGlowTodayPhotos({ page: 0, size: PHOTO_PAGE_SIZE }).then((response) => {
      if (cancelled) return;
      setPhotos(response.items);
      setPhotoHasNext(response.hasNext);
      setPhotoLoading(false);
    });
    void fetchGlowTodayReviews({ page: 0, size: REVIEW_COUNT }).then((response) => {
      if (cancelled) return;
      setReviews(response.items.slice(0, REVIEW_COUNT));
      setReviewLoading(false);
    });

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!activePhoto) return;
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setActivePhoto(null);
    };
    window.addEventListener('keydown', closeOnEscape);
    return () => window.removeEventListener('keydown', closeOnEscape);
  }, [activePhoto]);

  const loadMorePhotos = useCallback(() => {
    if (photoLoading) return;
    setPhotoLoading(true);
    if (!photoHasNext) {
      window.setTimeout(() => setPhotoLoading(false), 550);
      return;
    }
    const nextPage = photoPage + 1;
    void fetchGlowTodayPhotos({ page: nextPage, size: PHOTO_PAGE_SIZE }).then((response) => {
      setPhotos((current) => [...current, ...response.items]);
      setPhotoHasNext(response.hasNext);
      setPhotoPage(nextPage);
      setPhotoLoading(false);
    });
  }, [photoHasNext, photoLoading, photoPage]);


  return (
    <div className={styles.recentReviews}>
      {filterBar}

      <section aria-labelledby="today-photo-title">
        <h2 id="today-photo-title" className={styles.sectionTitle}>오늘의 사진</h2>
        <div className={styles.photoGrid}>
          {photos.map((photo) => (
            <button
              key={photo.id}
              type="button"
              className={styles.photoButton}
              aria-label={`${photo.placeName} 사진 크게 보기`}
              onClick={() => setActivePhoto(photo)}
            >
              <img src={photo.imageUrl} alt="" loading="lazy" />
            </button>
          ))}
        </div>
        <button
          type="button"
          className={glowStyles.moreButton}
          aria-label={photoLoading ? '사진 불러오는 중' : '사진 더 보기'}
          aria-busy={photoLoading}
          disabled={photoLoading}
          onClick={loadMorePhotos}
        >
          {photoLoading ? <span className={glowStyles.loadingSpinner} aria-hidden="true" /> : '+'}
        </button>
      </section>

      <Link to="/wish" className={styles.promoBanner}>
        <span className={styles.promoImages} aria-hidden="true">
          {PROMO_IMAGES.map((src) => (
            <img key={src} src={src} alt="" loading="lazy" />
          ))}
        </span>
        <span className={styles.promoText}>
          <strong>당신만의 K-POP 스타일을 발견하세요</strong>
          <span>넘버원 테크의 포트라</span>
        </span>
      </Link>

      <section aria-labelledby="today-review-title">
        <h2 id="today-review-title" className={styles.sectionTitle}>오늘의 리뷰</h2>
        <div className={styles.reviewGrid}>
          {reviews.map((review) => (
            <ReviewCard key={review.id} review={review} />
          ))}
        </div>
        {!reviewLoading && reviews.length === 0 ? (
          <p className={styles.empty} role="status">아직 등록된 리뷰가 없습니다.</p>
        ) : null}
      </section>

      {activePhoto ? (
        <div className={glowStyles.shareOverlay} role="presentation" onMouseDown={() => setActivePhoto(null)}>
          <figure
            className={styles.photoViewer}
            role="dialog"
            aria-modal="true"
            aria-label={`${activePhoto.placeName} 사진`}
            onMouseDown={(event) => event.stopPropagation()}
          >
            <button type="button" className={styles.viewerClose} aria-label="사진 닫기" onClick={() => setActivePhoto(null)}>
              ×
            </button>
            <img src={activePhoto.imageUrl.replace('w=480&h=300', 'w=1200&h=760')} alt={activePhoto.caption} />
            <figcaption>{activePhoto.placeName}</figcaption>
          </figure>
        </div>
      ) : null}
    </div>
  );
};

export default GlowRecentReviews;
