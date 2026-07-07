/**
 * ArticleListPage Component
 *
 * 웹진 목록 페이지 - 카드형 리스트, 무한 스크롤
 */

import { useState, useCallback, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import type { SortOption } from "../../../domains/article/domain/article.types";
import { useInfiniteArticles } from '../../hooks/useArticles';
import { useInfiniteScroll } from '../../hooks/useInfiniteScroll';
import { ArticleCard } from '../../components/ArticleCard/ArticleCard';
import { SortFilter } from '../../components/SortFilter/SortFilter';
import { LoadingSpinner } from '../../components/LoadingSpinner/LoadingSpinner';
import styles from './ArticleListPage.module.css';

type ArticleListPageProps = {
  /** Glow 등에서 래퍼 <main> 중복을 피할 때 */
  readonly embed?: boolean;
};

/**
 * ArticleListPage 컴포넌트
 */
export const ArticleListPage = ({ embed = false }: ArticleListPageProps) => {
  const [searchParams] = useSearchParams();
  const searchQuery = (searchParams.get('q') ?? '').trim();

  // 정렬 상태
  const [sortBy, setSortBy] = useState<SortOption>('latest');

  // 무한 스크롤 데이터 조회
  const { articles, loading, hasMore, error, loadMore } = useInfiniteArticles(sortBy, 6);

  const displayedArticles = useMemo(() => {
    if (!searchQuery) return articles;
    const lower = searchQuery.toLowerCase();
    return articles.filter(
      article =>
        article.title.toLowerCase().includes(lower) ||
        article.categoryLabel.toLowerCase().includes(lower) ||
        article.tags.some(tag => tag.toLowerCase().includes(lower))
    );
  }, [articles, searchQuery]);

  const listTitle = searchQuery
    ? `「${searchQuery}」 검색 ${displayedArticles.length}건`
    : `전체 ${articles.length}건`;

  // 무한 스크롤 옵저버
  const { observerRef } = useInfiniteScroll(loadMore, {
    threshold: 0.1,
    rootMargin: '200px',
  });

  // 정렬 변경 핸들러
  const handleSortChange = useCallback((newSort: SortOption) => {
    setSortBy(newSort);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }, []);

  const content = (
    <>
      <div className={styles.container}>
        {/* 페이지 헤더 */}
        <header className={styles.header}>
          <h1 className={styles.title}>아티클</h1>
          <p className={styles.description}>
            문화와 예술의 다양한 이야기를 만나보세요.
            전시, 공연, 아카이브 등 풍성한 콘텐츠가 준비되어 있습니다.
          </p>
        </header>

        {/* 정렬 필터 */}
        <SortFilter
          currentSort={sortBy}
          onSortChange={handleSortChange}
          title={listTitle}
        />

        {/* 에러 상태 */}
        {error && (
          <div role="alert" className={styles.error}>
            <p>아티클을 불러오는 중 오류가 발생했습니다.</p>
          </div>
        )}

        {/* 아티클 그리드 */}
        <div className={styles.grid}>
          {displayedArticles.map(article => (
            <ArticleCard key={article.id} article={article} />
          ))}
        </div>

        {!loading && searchQuery && displayedArticles.length === 0 && (
          <p className={styles.emptySearch} role="status">
            「{searchQuery}」에 맞는 아티클이 없습니다.
          </p>
        )}

        {/* 무한 스크롤 트리거 */}
        <div ref={observerRef} className={styles.scrollTrigger}>
          {loading && <LoadingSpinner text="아티클을 불러오는 중..." />}
          {!loading && !hasMore && articles.length > 0 && (
            <p className={styles.endMessage}>모든 아티클을 불러왔습니다.</p>
          )}
        </div>
      </div>
    </>
  );

  if (embed) {
    return <div className={styles.embedRoot}>{content}</div>;
  }

  return (
    <main className={styles.main} id="main-content">
      {content}
    </main>
  );
};

export default ArticleListPage;
