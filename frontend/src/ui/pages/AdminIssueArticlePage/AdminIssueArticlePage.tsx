import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import {
  deleteAdminIssueArticle,
  fetchAdminIssueArticle,
  fetchAdminIssueVolume,
  type AdminIssueArticle,
  type AdminIssueVolume,
} from '../../../shared/data/issueAdminApi';
import { IssueBadge } from '../../components/IssueBadge/IssueBadge';
import styles from './AdminIssueArticlePage.module.css';

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

const ArrowLeftIcon = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
    <path d="m15 18-6-6 6-6" />
  </svg>
);

const ArrowRightIcon = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
    <path d="m9 18 6-6-6-6" />
  </svg>
);

const RELATED_VISIBLE_COUNT = 3;
const RELATED_AUTO_SLIDE_DELAY = 3200;

export const AdminIssueArticlePage = () => {
  const navigate = useNavigate();
  const { issueSlug = '', articleSlug = '' } = useParams();
  const { accessToken, authReady } = useAuth();
  const [issue, setIssue] = useState<AdminIssueVolume | null>(null);
  const [article, setArticle] = useState<AdminIssueArticle | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const galleryImages = useMemo(() => {
    if (!issue || !article) {
      return [];
    }

    return Array.from(
      new Set([
        ...article.galleryImageUrls,
        article.heroImageUrl,
        article.cardImageUrl,
        issue.coverImageUrl,
        ...article.relatedContents.map(content => content.imageUrl),
      ])
    );
  }, [article, issue]);

  const [activeSlide, setActiveSlide] = useState(0);
  const [relatedIndex, setRelatedIndex] = useState(0);
  const [isRelatedTransitionEnabled, setIsRelatedTransitionEnabled] = useState(true);

  useEffect(() => {
    if (!authReady) {
      return;
    }

    if (!accessToken) {
      setError('관리자 API를 보려면 로그인해야 합니다.');
      setIssue(null);
      setArticle(null);
      return;
    }

    let cancelled = false;

    const run = async () => {
      setLoading(true);
      setError(null);

      try {
        const [issueResponse, articleResponse] = await Promise.all([
          fetchAdminIssueVolume(accessToken, issueSlug),
          fetchAdminIssueArticle(accessToken, issueSlug, articleSlug),
        ]);

        if (!cancelled) {
          setIssue(issueResponse);
          setArticle(articleResponse);
          setActiveSlide(0);
          setIsRelatedTransitionEnabled(true);
        }
      } catch (fetchError) {
        if (!cancelled) {
          setError(fetchError instanceof Error ? fetchError.message : '기사 상세를 불러오지 못했습니다.');
          setIssue(null);
          setArticle(null);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    void run();

    return () => {
      cancelled = true;
    };
  }, [accessToken, articleSlug, authReady, issueSlug]);

  const relatedContents = article?.relatedContents ?? [];
  const isVideoFormat = article?.formatLabel.includes('비디오') ?? false;
  const shouldAutoSlideRelated = relatedContents.length >= RELATED_VISIBLE_COUNT;
  const relatedCarouselItems = useMemo(() => {
    if (!shouldAutoSlideRelated) {
      return relatedContents;
    }

    return [
      ...relatedContents.slice(-RELATED_VISIBLE_COUNT),
      ...relatedContents,
      ...relatedContents.slice(0, RELATED_VISIBLE_COUNT),
    ];
  }, [relatedContents, shouldAutoSlideRelated]);
  const relatedInitialIndex = shouldAutoSlideRelated ? RELATED_VISIBLE_COUNT : 0;

  useEffect(() => {
    setRelatedIndex(relatedInitialIndex);
  }, [relatedInitialIndex]);

  useEffect(() => {
    if (!shouldAutoSlideRelated) {
      return;
    }

    const intervalId = window.setInterval(() => {
      setIsRelatedTransitionEnabled(true);
      setRelatedIndex(current => current + 1);
    }, RELATED_AUTO_SLIDE_DELAY);

    return () => {
      window.clearInterval(intervalId);
    };
  }, [shouldAutoSlideRelated]);

  useEffect(() => {
    if (isRelatedTransitionEnabled) {
      return;
    }

    const frameId = window.requestAnimationFrame(() => {
      setIsRelatedTransitionEnabled(true);
    });

    return () => {
      window.cancelAnimationFrame(frameId);
    };
  }, [isRelatedTransitionEnabled]);

  const moveSlide = (direction: 'prev' | 'next') => {
    if (galleryImages.length <= 1) {
      return;
    }

    setActiveSlide(current =>
      direction === 'prev'
        ? (current - 1 + galleryImages.length) % galleryImages.length
        : (current + 1) % galleryImages.length
    );
  };

  const handleDelete = async () => {
    if (!accessToken || !issue || !article) {
      return;
    }

    const confirmed = window.confirm('이 기사를 삭제할까요? 삭제 후에는 목록 페이지로 돌아갑니다.');

    if (!confirmed) {
      return;
    }

    try {
      await deleteAdminIssueArticle(accessToken, issue.slug, article.slug);
      navigate(`/admin/issues/${issue.slug}`);
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : '기사 삭제에 실패했습니다.');
    }
  };

  const moveRelated = (direction: 'prev' | 'next') => {
    if (!shouldAutoSlideRelated) {
      return;
    }

    setIsRelatedTransitionEnabled(true);
    setRelatedIndex(current => current + (direction === 'next' ? 1 : -1));
  };

  const handleRelatedTrackTransitionEnd = () => {
    if (!shouldAutoSlideRelated) {
      return;
    }

    if (relatedIndex >= relatedContents.length + RELATED_VISIBLE_COUNT) {
      setIsRelatedTransitionEnabled(false);
      setRelatedIndex(previous => previous - relatedContents.length);
      return;
    }

    if (relatedIndex < RELATED_VISIBLE_COUNT) {
      setIsRelatedTransitionEnabled(false);
      setRelatedIndex(previous => previous + relatedContents.length);
    }
  };

  if (loading) {
    return (
      <main className={styles.page} id="main-content">
        <div className={styles.container}>
          <Link to="/admin/issues" className={styles.backLink}>
            트랜드 보기로 돌아가기
          </Link>
          <div className={styles.empty}>기사 상세를 불러오는 중입니다.</div>
        </div>
      </main>
    );
  }

  if (!issue || !article) {
    return (
      <main className={styles.page} id="main-content">
        <div className={styles.container}>
          <Link to="/admin/issues" className={styles.backLink}>
            트랜드 보기로 돌아가기
          </Link>
          <div className={styles.empty}>{error ?? '상세 미리보기 데이터를 찾을 수 없습니다.'}</div>
        </div>
      </main>
    );
  }

  return (
    <main className={styles.page} id="main-content">
      <div className={styles.container}>
        <div className={styles.topRow}>
          <Link to={`/admin/issues/${issue.slug}`} className={styles.backLink}>
            Vol.{issue.volume} 트랜드 목록으로 돌아가기
          </Link>

          <div className={styles.adminActionGroup}>
            <button
              type="button"
              className={styles.secondaryButton}
              onClick={() => navigate(`/admin/issues/${issue.slug}/articles/${article.slug}/edit`)}
            >
              수정
            </button>
            <button type="button" className={styles.dangerButton} onClick={handleDelete}>
              삭제
            </button>
          </div>
        </div>

        {error ? <div className={styles.empty}>{error}</div> : null}

        <section className={styles.heroSliderSection}>
          <div className={styles.heroViewport}>
            <div
              className={styles.heroTrack}
              style={{ transform: `translateX(-${activeSlide * 100}%)` }}
            >
              {galleryImages.map((imageUrl, index) => (
                <div key={`${imageUrl}-${index}`} className={styles.heroSlide}>
                  <img
                    src={imageUrl}
                    alt={`${article.title} 이미지 ${index + 1}`}
                    className={styles.heroImage}
                  />
                </div>
              ))}
            </div>

            {galleryImages.length > 1 ? (
              <>
                <button
                  type="button"
                  className={`${styles.slideButton} ${styles.slideButtonLeft}`}
                  onClick={() => moveSlide('prev')}
                  aria-label="이전 이미지"
                >
                  <ArrowLeftIcon />
                </button>
                <button
                  type="button"
                  className={`${styles.slideButton} ${styles.slideButtonRight}`}
                  onClick={() => moveSlide('next')}
                  aria-label="다음 이미지"
                >
                  <ArrowRightIcon />
                </button>
                <div className={styles.slideCounter}>
                  {String(activeSlide + 1).padStart(2, '0')} /{' '}
                  {String(galleryImages.length).padStart(2, '0')}
                </div>
              </>
            ) : null}
          </div>

          {galleryImages.length > 1 ? (
            <div className={styles.thumbnailRow}>
              {galleryImages.map((imageUrl, index) => (
                <button
                  key={`${imageUrl}-thumb-${index}`}
                  type="button"
                  className={`${styles.thumbnailButton} ${
                    index === activeSlide ? styles.thumbnailButtonActive : ''
                  }`}
                  onClick={() => setActiveSlide(index)}
                  aria-label={`${index + 1}번 이미지 보기`}
                >
                  <img src={imageUrl} alt="" className={styles.thumbnailImage} aria-hidden="true" />
                </button>
              ))}
            </div>
          ) : null}
        </section>

        <header className={styles.titleBlock}>
          <p className={styles.kicker}>{article.kicker}</p>
          <h1 className={styles.title}>{article.title}</h1>
        </header>

        <div className={styles.rule} />

        <section className={styles.summarySection}>
          <h2 className={styles.sectionLabel}>요약정보</h2>
          <p className={styles.summaryText}>{article.summary}</p>
        </section>

        <section className={styles.actionRow}>
          <IssueBadge category={article.category} />
          <IssueBadge category={isVideoFormat ? 'video' : 'article'} />
          <span className={styles.iconAction}>
            <span className={styles.iconCircle}>
              <HeartIcon />
            </span>
            공감
          </span>
          <span className={styles.iconAction}>
            <span className={styles.iconCircle}>
              <ShareIcon />
            </span>
            링크복사
          </span>
        </section>

        <div className={styles.tagRow}>
          {article.tags.map(tag => (
            <span key={tag} className={styles.tag}>
              {tag}
            </span>
          ))}
        </div>

        {article.highlightQuote ? <div className={styles.quoteBox}>{article.highlightQuote}</div> : null}

        {article.sections.map(section => (
          <section key={section.id} className={styles.contentSection}>
            {section.heading ? <h2 className={styles.sectionHeading}>{section.heading}</h2> : null}

            {section.paragraphs.map(paragraph => (
              <p key={paragraph} className={styles.paragraph}>
                {paragraph}
              </p>
            ))}

            {section.note ? <div className={styles.noteBox}>{section.note}</div> : null}
          </section>
        ))}

        <div className={styles.rule} />

        <section className={styles.footerActions}>
          <Link to={`/admin/issues/${issue.slug}`} className={styles.listButton}>
            목록보기
          </Link>

          <p className={styles.credit}>
            by {article.authorName} ({article.authorEmail}) | {article.creditLine}
          </p>

          <div className={styles.metaActions}>
            <span className={styles.iconAction}>
              <span className={styles.iconCircle}>
                <HeartIcon />
              </span>
              공감
            </span>
            <span className={styles.iconAction}>
              <span className={styles.iconCircle}>
                <ShareIcon />
              </span>
              링크복사
            </span>
          </div>
        </section>

        <div className={styles.rule} />

        <section className={styles.relatedSection}>
          <div className={styles.relatedHeader}>
            <h2 className={styles.relatedTitle}>같이 보면 좋은 콘텐츠</h2>

            <div className={styles.carouselControls}>
              <button
                type="button"
                className={styles.carouselButton}
                onClick={() => moveRelated('prev')}
                disabled={!shouldAutoSlideRelated}
                aria-label="이전 관련 콘텐츠"
              >
                <ArrowLeftIcon />
              </button>
              <button
                type="button"
                className={styles.carouselButton}
                onClick={() => moveRelated('next')}
                disabled={!shouldAutoSlideRelated}
                aria-label="다음 관련 콘텐츠"
              >
                <ArrowRightIcon />
              </button>
            </div>
          </div>

          <div className={styles.relatedViewport}>
            <div
              className={styles.relatedTrack}
              style={{
                transform: `translateX(calc(-${relatedIndex} * (var(--related-card-width) + var(--related-gap))))`,
                transition: isRelatedTransitionEnabled ? 'transform 420ms ease' : 'none',
              }}
              onTransitionEnd={handleRelatedTrackTransitionEnd}
            >
              {relatedCarouselItems.map((content, index) => (
                <article key={`${content.id}-${index}`} className={styles.relatedCard}>
                  <img src={content.imageUrl} alt={content.title} className={styles.relatedImage} />
                  <IssueBadge category={content.category} />
                  <h3 className={styles.relatedCardTitle}>{content.title}</h3>
                  <div className={styles.relatedRule} />
                </article>
              ))}
            </div>
          </div>
        </section>
      </div>
    </main>
  );
};

export default AdminIssueArticlePage;
