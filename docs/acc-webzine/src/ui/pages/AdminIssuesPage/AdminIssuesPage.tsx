import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import { fetchAdminIssuePage, type AdminIssueSummary } from '../../../shared/data/issueAdminApi';
import '../../../../../samples/portal-stat-bar.css';
import { AdminMarketingShell } from '../AdminMarketingPage/AdminMarketingShell';
import shellStyles from '../AdminMarketingPage/AdminMarketingPage.module.css';
import shellToolbar from '../AdminUsersPage/AdminUsersShellToolbar.module.css';
import styles from './AdminIssuesPage.module.css';

const PAGE_SIZE = 5;
const PAGE_NUMBER_GROUP_SIZE = 5;

const ArrowIcon = ({ direction }: { readonly direction: 'left' | 'right' }) => (
  <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path
      d={direction === 'left' ? 'M15 6L9 12L15 18' : 'M9 6L15 12L9 18'}
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>
);

const IssueStatCount = ({ n }: { readonly n: number }) => (
  <>
    <span className={shellToolbar.statNumber}>{n}</span>
    <span className={shellToolbar.statUnit}>건</span>
  </>
);

export function AdminIssuesPage() {
  const navigate = useNavigate();
  const { accessToken, authReady, user } = useAuth();
  const [items, setItems] = useState<readonly AdminIssueSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [latestVolume, setLatestVolume] = useState(0);
  const [totalArticleCount, setTotalArticleCount] = useState(0);

  useEffect(() => {
    if (!authReady || !accessToken || user?.role !== 'ADMIN') {
      return;
    }

    let cancelled = false;

    const load = async () => {
      setLoading(true);
      setError(null);

      try {
        const response = await fetchAdminIssuePage(accessToken, {
          page: currentPage,
          size: PAGE_SIZE,
        });

        if (cancelled) {
          return;
        }

        setItems(response.issues);
        setTotalElements(response.totalElements);
        setTotalPages(Math.max(1, response.totalPages || 1));
        setLatestVolume(response.latestVolume);
        setTotalArticleCount(response.totalArticleCount);
      } catch (loadError) {
        if (!cancelled) {
          setError(loadError instanceof Error ? loadError.message : '트랜드 보기 목록을 불러오지 못했습니다.');
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    void load();

    return () => {
      cancelled = true;
    };
  }, [accessToken, authReady, currentPage, user?.role]);

  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  const currentGroupStart =
    Math.floor((currentPage - 1) / PAGE_NUMBER_GROUP_SIZE) * PAGE_NUMBER_GROUP_SIZE + 1;
  const currentGroupEnd = Math.min(
    currentGroupStart + PAGE_NUMBER_GROUP_SIZE - 1,
    Math.max(1, totalPages)
  );
  const visiblePageNumbers = Array.from(
    { length: currentGroupEnd - currentGroupStart + 1 },
    (_, index) => currentGroupStart + index
  );

  const publishedCountOnPage = useMemo(
    () => items.reduce((count, item) => count + item.articleCount, 0),
    [items]
  );

  if (!authReady) {
    return (
      <main className={shellStyles.page}>
        <div className={shellStyles.loading}>관리자 권한을 확인하는 중입니다.</div>
      </main>
    );
  }

  if (!accessToken || user?.role !== 'ADMIN') {
    return (
      <main className={shellStyles.page}>
        <div className={shellStyles.denied}>관리자만 트랜드 보기를 확인하고 관리할 수 있습니다.</div>
      </main>
    );
  }

  return (
    <AdminMarketingShell
      currentPath="/admin/issues"
      title="트랜드 보기"
      description=""
      classNames={{
        toolbarCard: shellToolbar.toolbarCard,
        header: shellToolbar.header,
        title: shellToolbar.title,
        statusText: shellToolbar.statusText,
        stats: shellToolbar.stats,
        statCard: shellToolbar.statCard,
        statLabel: shellToolbar.statLabel,
        statValue: shellToolbar.statValue,
      }}
      statusText={error ? error : loading ? '트랜드 보기 목록을 불러오는 중입니다.' : null}
      stats={[
        { label: '총 트랜드 수', value: <IssueStatCount n={totalElements} /> },
        {
          label: '최신 트랜드',
          value: latestVolume > 0 ? <IssueStatCount n={latestVolume} /> : <span className={shellToolbar.statUnit}>-</span>,
        },
        { label: '전체 기사', value: <IssueStatCount n={totalArticleCount} /> },
      ]}
    >
      <section className={styles.panel}>
        <div className={styles.panelHeader}>
          <h2 className={styles.panelTitle}>등록된 트랜드 보기</h2>
          <button
            type="button"
            className={styles.primaryButton}
            onClick={() => navigate('/admin/issues/new')}
          >
            트랜드 등록
          </button>
        </div>
        <div className={styles.panelBody}>
          {loading ? (
            <div className={styles.empty}>트랜드 보기 목록을 불러오는 중입니다.</div>
          ) : error ? (
            <div className={styles.empty}>{error}</div>
          ) : items.length === 0 ? (
            <div className={styles.empty}>등록된 트랜드가 없습니다.</div>
          ) : (
            <>
              <div className={styles.list}>
                {items.map(item => (
                  <article key={item.id} className={styles.card}>
                    <div className={styles.thumb}>
                      {item.coverImageUrl ? (
                        <img src={item.coverImageUrl} alt={`Vol.${item.volume} 표지`} />
                      ) : (
                        <div className={styles.thumbFallback}>이미지 없음</div>
                      )}
                    </div>
                    <div className={styles.cardBody}>
                      <div className={styles.topRow}>
                        <div>
                          <h3 className={styles.name}>Vol.{item.volume}</h3>
                          <p className={styles.meta}>{item.issueDate}</p>
                        </div>
                        <div className={styles.rowActions}>
                          <Link to={`/admin/issues/${item.slug}`} className={styles.secondaryButton}>
                            상세
                          </Link>
                          <Link to={`/admin/issues/${item.slug}/edit`} className={styles.primaryButton}>
                            수정
                          </Link>
                        </div>
                      </div>
                      <div className={styles.chips}>
                        <span className={styles.chip}>기사 {item.articleCount}건</span>
                        <span className={styles.chip}>{item.issueYear}.{String(item.issueMonth).padStart(2, '0')}</span>
                      </div>
                      {item.teaser ? <p className={styles.meta}>{item.teaser}</p> : null}
                      {item.headlines.length > 0 ? (
                        <p className={styles.meta}>
                          {item.headlines.map(headline => headline.title).join(' / ')}
                        </p>
                      ) : null}
                    </div>
                  </article>
                ))}
              </div>

              <p className={styles.resultMeta}>현재 페이지 기사 수 {publishedCountOnPage}건</p>

              {totalPages > 1 ? (
                <div className={styles.pagination}>
                  <button
                    type="button"
                    className={styles.pageArrowButton}
                    onClick={() => setCurrentPage(previous => Math.max(1, previous - 1))}
                    disabled={currentPage <= 1}
                    aria-label="이전 페이지"
                  >
                    <ArrowIcon direction="left" />
                  </button>

                  <div className={styles.paginationNumbers}>
                    {visiblePageNumbers.map(pageNumber => (
                      <button
                        key={pageNumber}
                        type="button"
                        className={[
                          styles.pageButton,
                          pageNumber === currentPage ? styles.pageButtonActive : '',
                        ]
                          .filter(Boolean)
                          .join(' ')}
                        onClick={() => setCurrentPage(pageNumber)}
                        aria-current={pageNumber === currentPage ? 'page' : undefined}
                      >
                        {pageNumber}
                      </button>
                    ))}
                  </div>

                  <button
                    type="button"
                    className={styles.pageArrowButton}
                    onClick={() =>
                      setCurrentPage(previous => Math.min(previous + 1, Math.max(1, totalPages)))
                    }
                    disabled={currentPage >= Math.max(1, totalPages)}
                    aria-label="다음 페이지"
                  >
                    <ArrowIcon direction="right" />
                  </button>
                </div>
              ) : null}
            </>
          )}
        </div>
      </section>
    </AdminMarketingShell>
  );
}

export default AdminIssuesPage;
