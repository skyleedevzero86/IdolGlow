import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import '../../../../../samples/portal-stat-bar.css';
import {
  deleteAdminNewsletter,
  fetchAdminNewsletterPage,
  type AdminNewsletterSummary,
} from '../../../shared/data/newsletterAdminApi';
import { AdminMarketingShell } from '../AdminMarketingPage/AdminMarketingShell';
import shellStyles from '../AdminMarketingPage/AdminMarketingPage.module.css';
import shellToolbar from '../AdminUsersPage/AdminUsersShellToolbar.module.css';
import styles from './AdminNewslettersPage.module.css';

const PAGE_SIZE = 10;
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

const NewsletterStatCount = ({ n }: { readonly n: number }) => (
  <>
    <span className={shellToolbar.statNumber}>{n}</span>
    <span className={shellToolbar.statUnit}>건</span>
  </>
);

const NewsletterStatPage = ({ page, totalPages }: { readonly page: number; readonly totalPages: number }) => (
  <>
    <span className={shellToolbar.statNumber}>{page}</span>
    <span className={shellToolbar.statUnit}>/ {totalPages}</span>
  </>
);

export function AdminNewslettersPage() {
  const navigate = useNavigate();
  const { accessToken, authReady, user } = useAuth();
  const [items, setItems] = useState<readonly AdminNewsletterSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  const loadItems = async (page = currentPage) => {
    if (!accessToken) {
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const response = await fetchAdminNewsletterPage(accessToken, page, PAGE_SIZE);
      setItems(response.newsletters);
      setTotalElements(response.totalElements);
      setTotalPages(Math.max(1, response.totalPages || 1));
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : '뉴스레터 목록을 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!authReady || !accessToken || user?.role !== 'ADMIN') {
      return;
    }
    void loadItems(currentPage);
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

  const tagCountOnPage = useMemo(
    () => items.reduce((count, item) => count + item.tags.length, 0),
    [items]
  );

  const handleDelete = async (newsletter: AdminNewsletterSummary) => {
    if (!accessToken || !window.confirm(`'${newsletter.title}'을 삭제할까요?`)) {
      return;
    }

    setError(null);
    try {
      await deleteAdminNewsletter(accessToken, newsletter.slug);
      await loadItems(currentPage);
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : '뉴스레터를 삭제하지 못했습니다.');
    }
  };

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
        <div className={shellStyles.denied}>관리자만 뉴스레터를 확인하고 관리할 수 있습니다.</div>
      </main>
    );
  }

  return (
    <AdminMarketingShell
      currentPath="/admin/newsletters"
      title="뉴스레터 관리"
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
      statusText={
        error
          ? error
          : loading
            ? '뉴스레터 목록을 불러오는 중입니다.'
            : null
      }
      stats={[
        { label: '총 뉴스레터', value: <NewsletterStatCount n={totalElements} /> },
        { label: '현재 페이지', value: <NewsletterStatPage page={currentPage} totalPages={Math.max(1, totalPages)} /> },
        { label: '태그 수', value: <NewsletterStatCount n={tagCountOnPage} /> },
      ]}
    >
      <section className={styles.panel}>
        <div className={styles.panelHeader}>
          <h2 className={styles.panelTitle}>등록된 뉴스레터</h2>
          <button
            type="button"
            className={styles.primaryButton}
            onClick={() => navigate('/admin/newsletters/new')}
          >
            새 뉴스레터 등록
          </button>
        </div>
        <div className={styles.panelBody}>
          {loading ? (
            <div className={styles.empty}>뉴스레터 목록을 불러오는 중입니다.</div>
          ) : error ? (
            <div className={styles.empty}>{error}</div>
          ) : items.length === 0 ? (
            <div className={styles.empty}>등록된 뉴스레터가 없습니다.</div>
          ) : (
            <>
              <div className={styles.list}>
                {items.map(item => (
                  <article key={item.id} className={styles.card}>
                    <div className={styles.thumb}>
                      {item.imageUrl ? (
                        <img src={item.imageUrl} alt={item.title} />
                      ) : (
                        <div className={styles.thumbFallback}>이미지 없음</div>
                      )}
                    </div>
                    <div className={styles.cardBody}>
                      <div className={styles.topRow}>
                        <div>
                          <h3 className={styles.name}>{item.title}</h3>
                          <p className={styles.meta}>{item.publishedAt}</p>
                        </div>
                        <div className={styles.rowActions}>
                          <Link
                            to={`/admin/newsletters/${item.slug}`}
                            className={styles.secondaryButton}
                          >
                            보기
                          </Link>
                          <Link
                            to={`/admin/newsletters/${item.slug}/edit`}
                            className={styles.secondaryButton}
                          >
                            수정
                          </Link>
                          <button
                            type="button"
                            className={styles.dangerButton}
                            onClick={() => handleDelete(item)}
                          >
                            삭제
                          </button>
                        </div>
                      </div>
                      <div className={styles.chips}>
                        <span className={styles.chip}>{item.categoryLabel}</span>
                        {item.tags.slice(0, 4).map(tag => (
                          <span key={`${item.slug}-${tag}`} className={styles.chip}>
                            #{tag}
                          </span>
                        ))}
                      </div>
                      <p className={styles.meta}>{item.summary}</p>
                    </div>
                  </article>
                ))}
              </div>

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

export default AdminNewslettersPage;
