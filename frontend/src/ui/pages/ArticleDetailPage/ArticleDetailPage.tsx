import { useParams, Link } from 'react-router-dom';
import { useArticleDetail, useAdjacentArticles } from '../../hooks/useArticles';
import { LoadingSpinner } from '../../components/LoadingSpinner/LoadingSpinner';
import styles from './ArticleDetailPage.module.css';

export const ArticleDetailPage = () => {
  const { id } = useParams<{ id: string }>();

  const { data: article, loading, error } = useArticleDetail(id || '');

  const { data: adjacent } = useAdjacentArticles(id || '');

  if (loading) {
    return (
      <main className={styles.main} id="main-content">
        <LoadingSpinner text="아티클을 불러오는 중..." />
      </main>
    );
  }

  if (error || !article) {
    return (
      <main className={styles.main} id="main-content">
        <div className={styles.errorContainer}>
          <h1 className={styles.errorTitle}>아티클을 찾을 수 없습니다</h1>
          <p className={styles.errorText}>
            요청하신 아티클이 존재하지 않거나 삭제되었습니다.
          </p>
          <Link to="/articles" className={styles.backButton}>
            목록으로 돌아가기
          </Link>
        </div>
      </main>
    );
  }

  return (
    <main className={styles.main} id="main-content">
      <header className={styles.hero}>
        <img
          src={article.thumbnailUrl}
          alt=""
          className={styles.heroImage}
        />
        <div className={styles.heroOverlay} aria-hidden="true" />

        <div className={styles.heroContent}>
          <span className={styles.categoryBadge}>{article.categoryLabel}</span>
          <h1 className={styles.heroTitle}>{article.title}</h1>

          <div className={styles.heroMeta}>
            <span className={styles.metaItem}>{article.volumeLabel}</span>
            <span className={styles.metaItem}>{article.publishDateFormatted}</span>
            <span className={styles.metaItem}>작성자: {article.authorName}</span>
          </div>
        </div>
      </header>

      <article className={styles.container}>
        <div
          className={styles.content}
          // biome-ignore lint/security/noDangerouslySetInnerHtml: Content is from trusted mock data
          dangerouslySetInnerHTML={{ __html: article.content }}
        />

        <div className={styles.tags} aria-label="관련 태그">
          {article.tags.map(tag => (
            <span key={tag} className={styles.tag}>
              {tag}
            </span>
          ))}
        </div>

        <nav className={styles.navigation} aria-label="이전/다음 글">
          {adjacent?.prev ? (
            <Link to={`/articles/${adjacent.prev.id}`} className={styles.navItem}>
              <span className={styles.navLabel}>이전 글</span>
              <span className={styles.navTitle}>{adjacent.prev.title}</span>
            </Link>
          ) : (
            <div className={`${styles.navItem} ${styles.navEmpty}`}>
              <span className={styles.navLabel}>이전 글</span>
              <span className={styles.navTitle}>없음</span>
            </div>
          )}

          {adjacent?.next ? (
            <Link to={`/articles/${adjacent.next.id}`} className={`${styles.navItem} ${styles.navItemNext}`}>
              <span className={styles.navLabel}>다음 글</span>
              <span className={styles.navTitle}>{adjacent.next.title}</span>
            </Link>
          ) : (
            <div className={`${styles.navItem} ${styles.navItemNext} ${styles.navEmpty}`}>
              <span className={styles.navLabel}>다음 글</span>
              <span className={styles.navTitle}>없음</span>
            </div>
          )}
        </nav>
      </article>
    </main>
  );
};

export default ArticleDetailPage;
