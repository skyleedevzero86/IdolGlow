import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import {
  deleteAdminIssue,
  fetchAdminIssuePage,
  fetchAdminIssueVolume,
  type AdminIssuePageResponse,
  type AdminIssueVolume,
} from '../../../shared/data/issueAdminApi';
import { IssueBadge } from '../../components/IssueBadge/IssueBadge';
import styles from './AdminIssueArticlesPage.module.css';

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

const parseYear = (issueDate: string): string => issueDate.slice(0, 4);
const parseMonth = (issueDate: string): string => issueDate.slice(5, 7);

const buildEmptyOptions = (): AdminIssuePageResponse => ({
  issues: [],
  page: 1,
  size: 1,
  totalElements: 0,
  totalPages: 1,
  hasNext: false,
  latestVolume: 0,
  totalArticleCount: 0,
  availableYears: [],
  availableMonths: [],
  availableVolumes: [],
});

export const AdminIssueArticlesPage = () => {
  const { issueSlug = '' } = useParams();
  const navigate = useNavigate();
  const { accessToken, authReady } = useAuth();
  const [issue, setIssue] = useState<AdminIssueVolume | null>(null);
  const [options, setOptions] = useState<AdminIssuePageResponse>(buildEmptyOptions);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedYear, setSelectedYear] = useState('');
  const [selectedMonth, setSelectedMonth] = useState('');
  const [selectedVolume, setSelectedVolume] = useState('');

  useEffect(() => {
    if (!authReady) {
      return;
    }

    if (!accessToken) {
      setError('관리자 API를 보려면 로그인해야 합니다.');
      setIssue(null);
      setOptions(buildEmptyOptions());
      return;
    }

    let cancelled = false;

    const run = async () => {
      setLoading(true);
      setError(null);

      try {
        const [issueResponse, optionsResponse] = await Promise.all([
          fetchAdminIssueVolume(accessToken, issueSlug),
          fetchAdminIssuePage(accessToken, { page: 1, size: 1 }),
        ]);

        if (!cancelled) {
          setIssue(issueResponse);
          setOptions(optionsResponse);
          setSelectedYear(parseYear(issueResponse.issueDate));
          setSelectedMonth(parseMonth(issueResponse.issueDate));
          setSelectedVolume(String(issueResponse.volume));
        }
      } catch (fetchError) {
        if (!cancelled) {
          setError(fetchError instanceof Error ? fetchError.message : '트랜드 정보를 불러오지 못했습니다.');
          setIssue(null);
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
  }, [accessToken, authReady, issueSlug]);

  const moveToMatchedIssue = async (nextYear: string, nextMonth: string, nextVolume: string) => {
    if (!accessToken) {
      return;
    }

    try {
      const matchedPage = await fetchAdminIssuePage(accessToken, {
        page: 1,
        size: 1,
        year: nextYear ? Number(nextYear) : null,
        month: nextMonth ? Number(nextMonth) : null,
        volume: nextVolume ? Number(nextVolume) : null,
      });

      const matchedIssue = matchedPage.issues[0];

      if (matchedIssue) {
        navigate(`/admin/issues/${matchedIssue.slug}`);
      }
    } catch (moveError) {
      setError(moveError instanceof Error ? moveError.message : '선택한 트랜드로 이동하지 못했습니다.');
    }
  };

  const handleDeleteIssue = async () => {
    if (!accessToken || !issue) {
      return;
    }

    const confirmed = window.confirm(
      `Vol.${issue.volume}을 삭제할까요?\n이 트랜드에 포함된 기사도 함께 삭제됩니다.`
    );

    if (!confirmed) {
      return;
    }

    try {
      await deleteAdminIssue(accessToken, issue.slug);
      navigate('/admin/issues');
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : '트랜드 삭제에 실패했습니다.');
    }
  };

  const latestVolume = options.latestVolume;
  const yearOptions = useMemo(
    () => [...options.availableYears].sort((left, right) => right - left),
    [options.availableYears]
  );
  const monthOptions = useMemo(
    () => [...options.availableMonths].sort((left, right) => left - right),
    [options.availableMonths]
  );
  const volumeOptions = useMemo(
    () => [...options.availableVolumes].sort((left, right) => right - left),
    [options.availableVolumes]
  );

  if (loading) {
    return (
      <main className={styles.page} id="main-content">
        <div className={styles.container}>
          <Link to="/admin/issues" className={styles.backLink}>
            트랜드 보기로 돌아가기
          </Link>
          <div className={styles.statusCard}>트랜드 정보를 불러오는 중입니다.</div>
        </div>
      </main>
    );
  }

  if (!issue) {
    return (
      <main className={styles.page} id="main-content">
        <div className={styles.container}>
          <Link to="/admin/issues" className={styles.backLink}>
            트랜드 보기로 돌아가기
          </Link>
          <div className={styles.statusCard}>{error ?? '선택한 트랜드 정보를 찾을 수 없습니다.'}</div>
        </div>
      </main>
    );
  }

  return (
    <main className={styles.page} id="main-content">
      <div className={styles.container}>
        <Link to="/admin/issues" className={styles.backLink}>
          트랜드 보기로 돌아가기
        </Link>

        <header className={styles.header}>
          <div>
            <h1 className={styles.title}>Idol Glow 트랜드 보기</h1>
            <p className={styles.subtitle}>
              Vol.{issue.volume} · {issue.issueDate} · 기사 {issue.articleCount}건
            </p>
          </div>

          <div className={styles.headerActions}>
            <button
              type="button"
              className={styles.secondaryButton}
              onClick={() => navigate(`/admin/issues/${issue.slug}/edit`)}
            >
              트랜드 수정
            </button>
            <button type="button" className={styles.dangerButton} onClick={handleDeleteIssue}>
              트랜드 삭제
            </button>
            <button
              type="button"
              className={styles.articleCreateButton}
              onClick={() => navigate(`/admin/issues/${issue.slug}/articles/new`)}
            >
              기사 등록
            </button>
          </div>
        </header>

        <div className={styles.filterPanel}>
          <div className={styles.volCate}>
            <div className={styles.secWrap}>
              <form className={styles.searchForm} onSubmit={event => event.preventDefault()}>
                <div className={styles.volSelectWrap}>
                  <div className={styles.selectBox}>
                    <label className={styles.srOnly} htmlFor="select-vol-year">
                      발행 연도
                    </label>
                    <select
                      id="select-vol-year"
                      name="search_year"
                      className={styles.selectVol}
                      value={selectedYear}
                      onChange={event => {
                        const nextYear = event.target.value;
                        setSelectedYear(nextYear);
                        void moveToMatchedIssue(nextYear, selectedMonth, selectedVolume);
                      }}
                    >
                      <option value="">발행 연도</option>
                      {yearOptions.map(year => (
                        <option key={year} value={year}>
                          {year}년
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className={styles.selectBox}>
                    <label className={styles.srOnly} htmlFor="select-vol-month">
                      발행 월
                    </label>
                    <select
                      id="select-vol-month"
                      name="search_month"
                      className={styles.selectVol}
                      value={selectedMonth}
                      onChange={event => {
                        const nextMonth = event.target.value;
                        setSelectedMonth(nextMonth);
                        void moveToMatchedIssue(selectedYear, nextMonth, selectedVolume);
                      }}
                    >
                      <option value="">발행 월</option>
                      {monthOptions.map(month => (
                        <option key={month} value={String(month).padStart(2, '0')}>
                          {String(month).padStart(2, '0')}월
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className={styles.selectBox}>
                    <label className={styles.srOnly} htmlFor="select-vol-num">
                      발행 트랜드
                    </label>
                    <select
                      id="select-vol-num"
                      name="search_vol"
                      className={styles.selectVol}
                      value={selectedVolume}
                      onChange={event => {
                        const nextVolume = event.target.value;
                        setSelectedVolume(nextVolume);
                        void moveToMatchedIssue(selectedYear, selectedMonth, nextVolume);
                      }}
                    >
                      <option value="">발행 트랜드</option>
                      {volumeOptions.map(volume => (
                        <option key={volume} value={String(volume)}>
                          트랜드 {volume}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
              </form>

              <div className={styles.countWrap}>
                <ul className={styles.countList}>
                  <li>
                    최신 트랜드 Vol. <span className={styles.latest}>{latestVolume}</span>
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>

        <div className={styles.rule} />

        {error ? <div className={styles.statusCard}>{error}</div> : null}

        {issue.articles.length === 0 ? (
          <section className={styles.emptyState}>
            <h2 className={styles.emptyStateTitle}>아직 등록된 기사가 없습니다.</h2>
            <p className={styles.emptyStateText}>
              먼저 선택한 트랜드의 기사 내용을 등록해 주세요. 등록 후 카드 목록과 상세보기가 연결됩니다.
            </p>
            <button
              type="button"
              className={styles.emptyStateButton}
              onClick={() => navigate(`/admin/issues/${issue.slug}/articles/new`)}
            >
              첫 기사 등록하기
            </button>
          </section>
        ) : (
          <section className={styles.grid}>
            {issue.articles.map(article => (
              <article key={article.id} className={styles.card}>
                <Link
                  to={`/admin/issues/${issue.slug}/articles/${article.slug}`}
                  className={styles.cardImageLink}
                >
                  <img src={article.cardImageUrl} alt={article.title} className={styles.cardImage} />
                </Link>

                <div className={styles.cardBody}>
                  <div className={styles.cardTop}>
                    <IssueBadge category={article.category} />
                    <div className={styles.actionGroup}>
                      <button type="button" className={styles.iconButton} aria-label="공감">
                        <HeartIcon />
                      </button>
                      <button type="button" className={styles.iconButton} aria-label="공유">
                        <ShareIcon />
                      </button>
                    </div>
                  </div>

                  <h2 className={styles.cardTitle}>
                    <Link
                      to={`/admin/issues/${issue.slug}/articles/${article.slug}`}
                      className={styles.titleLink}
                    >
                      {article.title}
                    </Link>
                  </h2>

                  <div className={styles.meta}>
                    <div className={styles.volume}>Vol. {article.volume}</div>
                    <div className={styles.tags}>
                      {article.tags.slice(0, 4).map(tag => (
                        <span key={tag} className={styles.tag}>
                          {tag}
                        </span>
                      ))}
                    </div>
                  </div>
                </div>
              </article>
            ))}
          </section>
        )}
      </div>
    </main>
  );
};

export default AdminIssueArticlesPage;
