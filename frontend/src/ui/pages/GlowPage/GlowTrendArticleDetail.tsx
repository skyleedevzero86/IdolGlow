import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  ApiError,
  apiErrorMessage,
  isAbortError,
} from "../../../shared/data/apiClient";
import {
  type IdolglowArticleDetail,
  fetchIdolglowArticle,
  idolglowArticleKey,
  idolglowArticlePath,
} from "../../../shared/data/idolglowPublicApi";
import { ContentComments } from "../../components/ContentComments/ContentComments";
import { useRecordContentView } from "../../hooks/useContentView";
import styles from "./GlowTrendDetail.module.css";

type LinkBuilder = (update: {
  readonly issue?: string | null;
  readonly article?: string | null;
}) => string;

type GlowTrendArticleDetailProps = {
  readonly issueSlug: string;
  readonly articleSlug: string;
  readonly linkTo: LinkBuilder;
  readonly onBack: () => void;
};

export function GlowTrendArticleDetail({
  issueSlug,
  articleSlug,
  linkTo,
  onBack,
}: GlowTrendArticleDetailProps) {
  const [article, setArticle] = useState<IdolglowArticleDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError(null);
    fetchIdolglowArticle(issueSlug, articleSlug, controller.signal)
      .then((response) => {
        setArticle(response);
        setLoading(false);
      })
      .catch((loadError: unknown) => {
        if (isAbortError(loadError)) return;
        setArticle(null);
        setError(
          loadError instanceof ApiError && loadError.status === 404
            ? "발행되지 않았거나 찾을 수 없는 기사입니다."
            : apiErrorMessage(loadError, "기사를 불러오지 못했습니다."),
        );
        setLoading(false);
      });
    return () => controller.abort();
  }, [issueSlug, articleSlug]);

  const contentKey = idolglowArticleKey(issueSlug, articleSlug);
  const path = idolglowArticlePath(issueSlug, articleSlug);

  const viewEntry = useMemo(
    () =>
      article
        ? {
            contentType: "ARTICLE" as const,
            contentKey,
            title: article.title,
            path,
            imageUrl: article.cardImageUrl || article.heroImageUrl || null,
          }
        : null,
    [article, contentKey, path],
  );
  useRecordContentView(viewEntry);

  const gallery = useMemo(
    () =>
      article
        ? article.galleryImageUrls.filter(
            (url) => url && url !== article.heroImageUrl,
          )
        : [],
    [article],
  );

  return (
    <article className={styles.detail} aria-label="트렌드 기사">
      <div className={styles.toolbar}>
        <button type="button" className={styles.backButton} onClick={onBack}>
          ‹ 트렌드 목록
        </button>
        <Link
          to={linkTo({ issue: issueSlug, article: null })}
          className={styles.issueLink}
        >
          이 호의 기사 전체 보기
        </Link>
      </div>

      {loading ? (
        <output className={styles.status}>기사를 불러오는 중입니다.</output>
      ) : error || !article ? (
        <p className={styles.status} role="alert">
          {error ?? "기사를 불러오지 못했습니다."}
        </p>
      ) : (
        <>
          <header className={styles.articleHeader}>
            <p className={styles.meta}>
              <span className={styles.badge}>{article.categoryLabel}</span>
              <span className={styles.badgeOutline}>{article.formatLabel}</span>
              <strong>Vol.{article.volume}</strong>
              <span>{article.issueDate}</span>
            </p>
            <h2 className={styles.articleTitle}>{article.title}</h2>
            <p className={styles.articleKicker}>{article.kicker}</p>
          </header>

          <figure className={styles.hero}>
            <img src={article.heroImageUrl} alt="" />
          </figure>

          <p className={styles.summary}>{article.summary}</p>

          {article.highlightQuote ? (
            <blockquote className={styles.quote}>
              {article.highlightQuote}
            </blockquote>
          ) : null}

          <div className={styles.body}>
            {article.sections.map((section) => (
              <section key={section.order} className={styles.section}>
                {section.heading ? (
                  <h3 className={styles.sectionHeading}>{section.heading}</h3>
                ) : null}
                {section.paragraphs.map((paragraph, index) => (
                  <p
                    key={`${section.order}-${index}`}
                    className={styles.paragraph}
                  >
                    {paragraph}
                  </p>
                ))}
                {section.note ? (
                  <p className={styles.note}>{section.note}</p>
                ) : null}
              </section>
            ))}
          </div>

          {gallery.length > 0 ? (
            <ul className={styles.gallery} aria-label="기사 이미지">
              {gallery.map((url) => (
                <li key={url}>
                  <img src={url} alt="" loading="lazy" />
                </li>
              ))}
            </ul>
          ) : null}

          {article.tags.length > 0 ? (
            <ul className={styles.tags} aria-label="태그">
              {article.tags.map((tag) => (
                <li key={tag}>#{tag}</li>
              ))}
            </ul>
          ) : null}

          <p className={styles.credit}>
            <span>글 {article.authorName}</span>
            <span>{article.creditLine}</span>
          </p>

          {article.previousArticle || article.nextArticle ? (
            <nav className={styles.adjacent} aria-label="이전·다음 기사">
              {article.previousArticle ? (
                <Link
                  to={linkTo({
                    issue: article.issueSlug,
                    article: article.previousArticle.slug,
                  })}
                >
                  <span>이전 기사</span>
                  <strong>{article.previousArticle.title}</strong>
                </Link>
              ) : (
                <span />
              )}
              {article.nextArticle ? (
                <Link
                  to={linkTo({
                    issue: article.issueSlug,
                    article: article.nextArticle.slug,
                  })}
                  className={styles.next}
                >
                  <span>다음 기사</span>
                  <strong>{article.nextArticle.title}</strong>
                </Link>
              ) : null}
            </nav>
          ) : null}

          {article.relatedArticles.length > 0 ? (
            <section
              className={styles.related}
              aria-label="같은 호의 다른 기사"
            >
              <h3 className={styles.relatedTitle}>
                Vol.{article.volume}의 다른 기사
              </h3>
              <ul className={styles.cardGrid}>
                {article.relatedArticles.map((related) => (
                  <li key={related.slug} className={styles.card}>
                    <Link
                      to={linkTo({
                        issue: article.issueSlug,
                        article: related.slug,
                      })}
                      className={styles.cardLink}
                    >
                      <span className={styles.cardImage}>
                        <img src={related.cardImageUrl} alt="" loading="lazy" />
                      </span>
                      <span className={styles.cardBody}>
                        <span className={styles.badges}>
                          <span className={styles.badge}>
                            {related.categoryLabel}
                          </span>
                        </span>
                        <strong className={styles.cardTitle}>
                          {related.title}
                        </strong>
                      </span>
                    </Link>
                  </li>
                ))}
              </ul>
            </section>
          ) : null}

          <ContentComments
            targetType="ARTICLE"
            targetKey={contentKey}
            targetTitle={article.title}
            targetPath={path}
            className={styles.comments}
          />
        </>
      )}
    </article>
  );
}

export default GlowTrendArticleDetail;
