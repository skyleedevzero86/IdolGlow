/**
 * ArticleCard Component
 *
 * 아티클 카드 컴포넌트
 * ViewModel을 props로 받아 UI만 렌더링 (도메인 로직 없음)
 */

import { memo } from 'react';
import { Link } from 'react-router-dom';
import type { ArticleViewModel, CategoryType } from "../../../domains/article/domain/article.types";
import styles from './ArticleCard.module.css';

// Props 타입 정의
interface ArticleCardProps {
  readonly article: ArticleViewModel;
}

// 카테고리별 CSS 클래스 매핑 - 순수 함수
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

// 아이콘 컴포넌트
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

/**
 * ArticleCard 컴포넌트
 * memo를 사용하여 불필요한 리렌더링 방지
 */
export const ArticleCard = memo<ArticleCardProps>(({ article }) => {
  return (
    <article className={styles.card}>
      {/* 이미지 영역 */}
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

      {/* 콘텐츠 영역 */}
      <div className={styles.content}>
        {/* 메타 정보 */}
        <div className={styles.meta}>
          <span
            className={`${styles.categoryBadge} ${getCategoryClass(article.categoryType)}`}
          >
            {article.categoryLabel}
          </span>

          <div className={styles.actions}>
            <button
              type="button"
              className={styles.actionButton}
              aria-label="좋아요"
            >
              <HeartIcon />
            </button>
            <button
              type="button"
              className={styles.actionButton}
              aria-label="공유하기"
            >
              <ShareIcon />
            </button>
          </div>
        </div>

        {/* 제목 */}
        <h3 className={styles.title}>
          <Link
            to={`/articles/${article.id}`}
            className={styles.titleLink}
          >
            {article.title}
          </Link>
        </h3>

        {/* Volume 정보 */}
        <p className={styles.volume}>{article.volumeLabel}</p>

        {/* 태그 */}
        <div className={styles.tags} aria-label="태그 목록">
          {article.tags.slice(0, 4).map(tag => (
            <span key={tag} className={styles.tag}>
              {tag}
            </span>
          ))}
        </div>
      </div>
    </article>
  );
});

ArticleCard.displayName = 'ArticleCard';

export default ArticleCard;
