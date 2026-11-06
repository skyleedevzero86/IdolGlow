import { memo, useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import type { ArticleViewModel, CategoryType } from "../../../domains/article/domain/article.types";
import styles from './ArticleCard.module.css';

interface ArticleCardProps {
  readonly article: ArticleViewModel;
}

const getCategoryClass = (category: CategoryType): string => {
  const classMap: Record<CategoryType, string> = {
    exhibition: styles.categoryExhibition,
    performance: styles.categoryPerformance,
    exchange: styles.categoryExchange,
    archive: styles.categoryArchive,
    video: styles.categoryVideo,
    event: styles.categoryEvent,
  };
  return classMap[category];
};

const HeartIcon = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
    <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
  </svg>
);

const ShareIcon = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
    <circle cx="18" cy="5" r="3" />
    <circle cx="6" cy="12" r="3" />
    <circle cx="18" cy="19" r="3" />
    <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" />
    <line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
  </svg>
);

const LinkIcon = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
    <path d="M10 13a5 5 0 0 0 7.07.07l2-2a5 5 0 0 0-7.07-7.07l-1.15 1.15" />
    <path d="M14 11a5 5 0 0 0-7.07-.07l-2 2A5 5 0 0 0 12 20l1.15-1.15" />
  </svg>
);

export const ArticleCard = memo<ArticleCardProps>(({ article }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const { authReady, user } = useAuth();
  const [isLiked, setIsLiked] = useState(false);
  const [likeCount, setLikeCount] = useState(article.likeCount);
  const [shareOpen, setShareOpen] = useState(false);
  const [isCopied, setIsCopied] = useState(false);
  const articleUrl = new URL(`/articles/${article.id}`, window.location.origin).toString();

  useEffect(() => {
    if (!shareOpen) return;
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setShareOpen(false);
    };
    window.addEventListener('keydown', closeOnEscape);
    return () => window.removeEventListener('keydown', closeOnEscape);
  }, [shareOpen]);

  const openSharePopup = () => {
    setIsCopied(false);
    setShareOpen(true);
  };

  const copyArticleUrl = async () => {
    await navigator.clipboard.writeText(articleUrl);
    setIsCopied(true);
  };

  const toggleLike = () => {
    if (!authReady) return;
    if (!user) {
      const redirect = encodeURIComponent(`${location.pathname}${location.search}${location.hash}`);
      navigate(`/?login=1&redirect=${redirect}`);
      return;
    }

    setIsLiked((liked) => {
      setLikeCount((count) => Math.max(0, count + (liked ? -1 : 1)));
      return !liked;
    });
  };

  return (
    <>
    <article className={styles.card}>
      <Link
        to={`/articles/${article.id}`}
        className={styles.imageWrapper}
        tabIndex={-1}
        aria-hidden="true"
      >
        <img
          src={article.thumbnailUrl}
          alt=""
          className={styles.image}
          loading="lazy"
        />
      </Link>

      <div className={styles.content}>
        <div className={styles.meta}>
          <span
            className={`${styles.categoryBadge} ${getCategoryClass(article.categoryType)}`}
          >
            {article.categoryLabel}
          </span>

          <div className={styles.actions}>
            <button
              type="button"
              className={`${styles.actionButton} ${styles.likeButton} ${isLiked ? styles.liked : ''}`}
              aria-label={isLiked ? '좋아요 취소' : '좋아요'}
              aria-pressed={isLiked}
              onClick={toggleLike}
            >
              <HeartIcon />
              <span className={styles.likeCount} aria-hidden="true">{likeCount}</span>
            </button>
            <button
              type="button"
              className={styles.actionButton}
              aria-label="공유하기"
              onClick={openSharePopup}
            >
              <ShareIcon />
            </button>
          </div>
        </div>

        <h3 className={styles.title}>
          <Link
            to={`/articles/${article.id}`}
            className={styles.titleLink}
          >
            {article.title}
          </Link>
        </h3>

        <p className={styles.volume}>{article.volumeLabel}</p>

        <div className={styles.tags} aria-label="태그 목록">
          {article.tags.slice(0, 4).map(tag => (
            <span key={tag} className={styles.tag}>
              {tag}
            </span>
          ))}
        </div>
      </div>
    </article>
    {shareOpen ? createPortal(
      <div className={styles.shareOverlay} role="presentation" onMouseDown={() => setShareOpen(false)}>
        <section
          className={styles.sharePopup}
          role="dialog"
          aria-modal="true"
          aria-labelledby={`article-share-title-${article.id}`}
          onMouseDown={(event) => event.stopPropagation()}
        >
          <div className={styles.sharePopupHeader}>
            <h2 id={`article-share-title-${article.id}`}>공유하기</h2>
            <button type="button" aria-label="공유 창 닫기" onClick={() => setShareOpen(false)}>×</button>
          </div>
          <p className={styles.shareArticleTitle}>{article.title}</p>
          <div className={styles.shareUrlRow}>
            <input aria-label="게시물 주소" value={articleUrl} readOnly onFocus={(event) => event.currentTarget.select()} />
            <button type="button" aria-label="게시물 주소 복사" onClick={() => void copyArticleUrl()}>
              <LinkIcon />
            </button>
          </div>
          <p className={styles.copyStatus} role="status">{isCopied ? '주소가 복사되었습니다.' : ''}</p>
        </section>
      </div>,
      document.body,
    ) : null}
    </>
  );
});

ArticleCard.displayName = 'ArticleCard';

export default ArticleCard;
