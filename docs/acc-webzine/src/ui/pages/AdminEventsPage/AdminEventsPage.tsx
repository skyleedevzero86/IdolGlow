import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import {
  fetchAdminEvents,
  type AdminEventStatus,
  type AdminEventSummary,
} from '../../../shared/data/adminEventsApi';
import '../../../../../samples/portal-stat-bar.css';
import '../../../../../samples/portal-pagination.css';
import { AdminMarketingShell } from '../AdminMarketingPage/AdminMarketingShell';
import shellStyles from '../AdminMarketingPage/AdminMarketingPage.module.css';
import shellToolbar from '../AdminUsersPage/AdminUsersShellToolbar.module.css';
import styles from './AdminEventsPage.module.css';

const PAGE_SIZE = 10;
const PAGE_WINDOW = 5;
const META_START_PREFIX = 'event-start:';
const META_END_PREFIX = 'event-end:';
const DRAFT_PREFIX = '[임시저장-이벤트 시작] ';
const PROGRESS_PREFIX = '[이벤트 진행중] ';
const ENDED_PREFIX = '[이벤트종료] ';

type EventLabelInfo = {
  readonly label: string;
  readonly title: string;
};

const stripEventPrefixes = (title: string): string =>
  title
    .replace(DRAFT_PREFIX, '')
    .replace(PROGRESS_PREFIX, '')
    .replace(ENDED_PREFIX, '')
    .trim();

const resolveEventLabel = (item: AdminEventSummary): EventLabelInfo => {
  const rawTitle = stripEventPrefixes(item.title);
  if (item.status === 'draft') {
    return { label: DRAFT_PREFIX.trim(), title: rawTitle };
  }

  const now = new Date();
  const endTag = item.tags.find(tag => tag.startsWith(META_END_PREFIX));
  const endDate = endTag ? new Date(`${endTag.slice(META_END_PREFIX.length)}T23:59:59`) : null;
  if (endDate && !Number.isNaN(endDate.getTime()) && now.getTime() > endDate.getTime()) {
    return { label: ENDED_PREFIX.trim(), title: rawTitle };
  }

  return { label: PROGRESS_PREFIX.trim(), title: rawTitle };
};
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

const STATUS_OPTIONS: ReadonlyArray<{ readonly value: AdminEventStatus; readonly label: string }> = [
  { value: 'all', label: '전체' },
  { value: 'published', label: '게시' },
  { value: 'draft', label: '임시저장' },
];

export function AdminEventsPage() {
  const navigate = useNavigate();
  const { accessToken, authReady, user } = useAuth();
  const [items, setItems] = useState<readonly AdminEventSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [searchInput, setSearchInput] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<AdminEventStatus>('all');

  const loadItems = async ({
    page = currentPage,
    query = searchQuery,
    status = statusFilter,
  }: {
    readonly page?: number;
    readonly query?: string;
    readonly status?: AdminEventStatus;
  } = {}) => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetchAdminEvents(accessToken, {
        page,
        size: PAGE_SIZE,
        query,
        status,
      });
      setItems(response.items);
      setTotalElements(response.totalElements);
      const computedPages = Math.max(1, Math.ceil(response.totalElements / Math.max(1, response.size)));
      setTotalPages(computedPages);
      if (page > computedPages) {
        setCurrentPage(computedPages);
      }
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : '이벤트 목록을 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!authReady || user?.role !== 'ADMIN') {
      return;
    }
    void loadItems({ page: currentPage, query: searchQuery, status: statusFilter });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authReady, user?.role, accessToken, currentPage, searchQuery, statusFilter]);

  const pageNumbers = useMemo(() => {
    const start = Math.floor((currentPage - 1) / PAGE_WINDOW) * PAGE_WINDOW + 1;
    const end = Math.min(totalPages, start + PAGE_WINDOW - 1);
    return Array.from({ length: end - start + 1 }, (_, idx) => start + idx);
  }, [currentPage, totalPages]);

  const pageStartRow = totalElements === 0 ? 0 : (currentPage - 1) * PAGE_SIZE + 1;
  const pageEndRow = Math.min(currentPage * PAGE_SIZE, totalElements);

  const handleSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setCurrentPage(1);
    setSearchQuery(searchInput.trim());
  };

  const resetSearch = () => {
    setSearchInput('');
    setSearchQuery('');
    setStatusFilter('all');
    setCurrentPage(1);
  };

  if (!authReady) {
    return (
      <main className={shellStyles.page}>
        <div className={shellStyles.loading}>관리자 권한을 확인하는 중입니다.</div>
      </main>
    );
  }

  if (user?.role !== 'ADMIN') {
    return (
      <main className={shellStyles.page}>
        <div className={shellStyles.denied}>관리자만 이벤트 관리 화면을 사용할 수 있습니다.</div>
      </main>
    );
  }

  return (
    <AdminMarketingShell
      currentPath="/admin/events"
      title="이벤트관리"
      description=""
      classNames={{
        toolbarCard: shellToolbar.toolbarCard,
        header: shellToolbar.header,
        title: shellToolbar.title,
        statusText: shellToolbar.statusText,
      }}
      statusText={error ?? null}
      stats={[]}
    >
      <section className={styles.panel}>
        <div className={styles.panelBody}>
          <div className={styles.listToolbar}>
            <form className={styles.searchForm} onSubmit={handleSearch}>
              <select
                className={styles.select}
                value={statusFilter}
                onChange={event => {
                  setCurrentPage(1);
                  setStatusFilter(event.target.value as AdminEventStatus);
                }}
              >
                {STATUS_OPTIONS.map(option => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
              <input
                className={styles.searchInput}
                value={searchInput}
                onChange={event => setSearchInput(event.target.value)}
                placeholder="제목/소개 검색"
              />
              <button type="submit" className={styles.searchButton}>
                검색
              </button>
              <button type="button" className={styles.secondaryButton} onClick={resetSearch}>
                초기화
              </button>
              <button type="button" className={styles.primaryButton} onClick={() => navigate('/admin/events/new')}>
                이벤트 등록
              </button>
            </form>
          </div>

          {loading ? (
            <div className={styles.empty}>이벤트 목록을 불러오는 중입니다.</div>
          ) : items.length === 0 ? (
            <div className={styles.empty}>등록된 이벤트가 없습니다.</div>
          ) : (
            <div className={styles.list}>
              {items.map(item => (
                (() => {
                  const labelInfo = resolveEventLabel(item);
                  return (
                    <article
                      key={item.documentId}
                      className={styles.card}
                      role="button"
                      tabIndex={0}
                      onClick={() => navigate(`/admin/events/${item.documentId}`)}
                      onKeyDown={event => {
                        if (event.key === 'Enter' || event.key === ' ') {
                          navigate(`/admin/events/${item.documentId}`);
                        }
                      }}
                    >
                      <div className={styles.thumbWrap}>
                        {item.thumbnailImageUrl ? (
                          <img src={item.thumbnailImageUrl} alt={item.title} className={styles.thumb} />
                        ) : (
                          <div className={styles.thumbFallback}>썸네일 없음</div>
                        )}
                      </div>
                      <div className={styles.cardBody}>
                        <h3 className={styles.cardTitle}>
                          <span className={styles.eventLabel}>{labelInfo.label}</span> {labelInfo.title}
                        </h3>
                        {item.introduction ? <p className={styles.cardIntro}>{item.introduction}</p> : null}
                        <p className={styles.cardMeta}>{new Date(item.updatedAt).toLocaleString('ko-KR')}</p>
                      </div>
                    </article>
                  );
                })()
              ))}
            </div>
          )}

          {!loading && totalElements > 0 ? (
            <div className={styles.paginationWrap}>
              <div className={styles.pagination}>
                <button
                  type="button"
                  className={styles.pageArrowButton}
                  onClick={() => setCurrentPage(1)}
                  disabled={currentPage <= 1}
                  aria-label="첫 페이지"
                  title="첫 페이지"
                >
                  «
                </button>
                <button
                  type="button"
                  className={styles.pageArrowButton}
                  onClick={() => setCurrentPage(previous => Math.max(1, previous - 1))}
                  disabled={currentPage <= 1}
                  aria-label="이전 페이지"
                  title="이전 페이지"
                >
                  <ArrowIcon direction="left" />
                </button>
                <div className={styles.paginationNumbers}>
                  {pageNumbers.map(number => (
                    <button
                      key={number}
                      type="button"
                      className={`${styles.pageButton} ${number === currentPage ? styles.pageButtonActive : ''}`}
                      onClick={() => setCurrentPage(number)}
                      aria-current={number === currentPage ? 'page' : undefined}
                    >
                      {number}
                    </button>
                  ))}
                </div>
                <button
                  type="button"
                  className={styles.pageArrowButton}
                  onClick={() => setCurrentPage(previous => Math.min(totalPages, previous + 1))}
                  disabled={currentPage >= totalPages}
                  aria-label="다음 페이지"
                  title="다음 페이지"
                >
                  <ArrowIcon direction="right" />
                </button>
                <button
                  type="button"
                  className={styles.pageArrowButton}
                  onClick={() => setCurrentPage(totalPages)}
                  disabled={currentPage >= totalPages}
                  aria-label="마지막 페이지"
                  title="마지막 페이지"
                >
                  »
                </button>
              </div>
            </div>
          ) : null}

          <p className={styles.paginationInfo}>
            전체 <strong>{totalElements}</strong>건 · 페이지당 <strong>{PAGE_SIZE}</strong>건 ·{' '}
            <strong>
              {pageStartRow}-{pageEndRow}
            </strong>
            번째 표시 · <strong>{currentPage}</strong> / <strong>{totalPages}</strong> 페이지
          </p>
        </div>
      </section>
    </AdminMarketingShell>
  );
}

export default AdminEventsPage;
