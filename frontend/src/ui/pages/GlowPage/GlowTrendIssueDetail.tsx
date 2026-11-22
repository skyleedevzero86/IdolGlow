import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  ApiError,
  apiErrorMessage,
  isAbortError,
} from "../../../shared/data/apiClient";
import {
  type IdolglowIssueDetail,
  fetchIdolglowIssue,
} from "../../../shared/data/idolglowPublicApi";
import styles from "./GlowTrendDetail.module.css";

type LinkBuilder = (update: {
  readonly issue?: string | null;
  readonly article?: string | null;
}) => string;

type GlowTrendIssueDetailProps = {
  readonly issueSlug: string;
  readonly linkTo: LinkBuilder;
  readonly onBack: () => void;
};

export function GlowTrendIssueDetail({
  issueSlug,
  linkTo,
  onBack,
}: GlowTrendIssueDetailProps) {
  const [issue, setIssue] = useState<IdolglowIssueDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError(null);
    fetchIdolglowIssue(issueSlug, controller.signal)
      .then((response) => {
        setIssue(response);
        setLoading(false);
      })
      .catch((loadError: unknown) => {
        if (isAbortError(loadError)) return;
        setIssue(null);
        setError(
          loadError instanceof ApiError && loadError.status === 404
            ? "발행되지 않았거나 찾을 수 없는 호입니다."
            : apiErrorMessage(loadError, "호 정보를 불러오지 못했습니다."),
        );
        setLoading(false);
      });
    return () => controller.abort();
  }, [issueSlug]);

  return (
    <section className={styles.detail} aria-label="트렌드 호 상세">
      <button type="button" className={styles.backButton} onClick={onBack}>
        ‹ 트렌드 목록
      </button>

      {loading ? (
        <output className={styles.status}>호 정보를 불러오는 중입니다.</output>
      ) : error || !issue ? (
        <p className={styles.status} role="alert">
          {error ?? "호 정보를 불러오지 못했습니다."}
        </p>
      ) : (
        <>
          <header className={styles.issueHeader}>
            <div className={styles.issueCover}>
              <img src={issue.coverImageUrl} alt={`Vol.${issue.volume} 표지`} />
            </div>
            <div className={styles.issueIntro}>
              <p className={styles.meta}>
                <strong>Vol.{issue.volume}</strong>
                <span>{issue.issueDate}</span>
                <span>기사 {issue.articleCount}개</span>
              </p>
              <h2 className={styles.issueTitle}>트렌드 Vol.{issue.volume}</h2>
              <p className={styles.issueTeaser}>{issue.teaser}</p>
            </div>
          </header>

          <ul className={styles.cardGrid}>
            {issue.articles.map((article) => (
              <li key={article.slug} className={styles.card}>
                <Link
                  to={linkTo({ issue: issue.slug, article: article.slug })}
                  className={styles.cardLink}
                >
                  <span className={styles.cardImage}>
                    <img src={article.cardImageUrl} alt="" loading="lazy" />
                  </span>
                  <span className={styles.cardBody}>
                    <span className={styles.badges}>
                      <span className={styles.badge}>
                        {article.categoryLabel}
                      </span>
                      <span className={styles.badgeOutline}>
                        {article.formatLabel}
                      </span>
                    </span>
                    <strong className={styles.cardTitle}>
                      {article.title}
                    </strong>
                    <span className={styles.cardKicker}>{article.kicker}</span>
                    <span className={styles.cardSummary}>
                      {article.summary}
                    </span>
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  );
}

export default GlowTrendIssueDetail;
