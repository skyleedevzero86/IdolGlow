import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { useAuth } from '../../../auth/AuthContext';
import {
  fetchAdminReviews,
  hideAdminReview,
  type AdminProductReviewPageResponse,
  type AdminProductReviewSummary,
  type AdminReviewVisibility,
  unhideAdminReview,
} from '../../../shared/data/adminReviewApi';
import '../../../../../samples/portal-stat-bar.css';
import { AdminMarketingShell } from '../AdminMarketingPage/AdminMarketingShell';
import shellStyles from '../AdminMarketingPage/AdminMarketingPage.module.css';
import shellToolbar from '../AdminUsersPage/AdminUsersShellToolbar.module.css';
import styles from './AdminReviewsPage.module.css';

const PAGE_SIZE = 10;

const EMPTY_PAGE: AdminProductReviewPageResponse = {
  reviews: [],
  page: 1,
  size: PAGE_SIZE,
  totalElements: 0,
  totalPages: 1,
  hasNext: false,
};

const VISIBILITY_OPTIONS: ReadonlyArray<{ value: AdminReviewVisibility; label: string }> = [
  { value: 'ALL', label: '전체' },
  { value: 'VISIBLE', label: '공개만' },
  { value: 'HIDDEN', label: '비공개만' },
];

function formatCreatedAt(raw: string): string {
  try {
    const d = new Date(raw);
    if (Number.isNaN(d.getTime())) {
      return raw;
    }
    return d.toLocaleString('ko-KR');
  } catch {
    return raw;
  }
}

function ratingStars(rating: number): string {
  const r = Math.max(0, Math.min(5, Math.round(rating)));
  return `${'★'.repeat(r)}${'☆'.repeat(5 - r)}`;
}

const normalizePage = (response: AdminProductReviewPageResponse): AdminProductReviewPageResponse => {
  const safeSize = Math.max(1, response.size || PAGE_SIZE);
  const computedTotalPages = Math.max(1, Math.ceil(response.totalElements / safeSize));
  return {
    ...response,
    size: safeSize,
    totalPages: Math.max(1, response.totalPages || computedTotalPages),
  };
};

export function AdminReviewsPage() {
  const { accessToken, authReady, user } = useAuth();
  const [pageData, setPageData] = useState<AdminProductReviewPageResponse>(EMPTY_PAGE);
  const [loading, setLoading] = useState(false);
  const [actingId, setActingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [keywordInput, setKeywordInput] = useState('');
  const [searchKeyword, setSearchKeyword] = useState('');
  const [visibility, setVisibility] = useState<AdminReviewVisibility>('ALL');
  const [hideTarget, setHideTarget] = useState<AdminProductReviewSummary | null>(null);
  const [hideReason, setHideReason] = useState('');

  const loadReviews = async ({
    page = currentPage,
    keyword = searchKeyword,
  }: {
    readonly page?: number;
    readonly keyword?: string;
  } = {}) => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetchAdminReviews(accessToken, {
        page,
        size: PAGE_SIZE,
        keyword,
        visibility,
      });
      const normalized = normalizePage(response);
      setPageData(normalized);
      if (page > normalized.totalPages) {
        setCurrentPage(normalized.totalPages);
      }
      return normalized;
    } catch (loadError) {
      setError(
        loadError instanceof Error ? loadError.message : '리뷰 목록을 불러오지 못했습니다.'
      );
      return null;
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!authReady || user?.role !== 'ADMIN') {
      return;
    }
    void loadReviews({ page: currentPage, keyword: searchKeyword });
    // eslint-disable-next-line react-hooks/exhaustive-deps -- visibility/currentPage driven by dedicated handlers
  }, [authReady, user?.role, accessToken, visibility, currentPage, searchKeyword]);

  const refreshCurrentPage = async () => {
    await loadReviews({ page: currentPage, keyword: searchKeyword });
  };

  const handleSearchSubmit = (event: FormEvent) => {
    event.preventDefault();
    setCurrentPage(1);
    setSearchKeyword(keywordInput.trim());
  };

  const handleReset = () => {
    setKeywordInput('');
    setSearchKeyword('');
    setVisibility('ALL');
    setCurrentPage(1);
  };

  const handleUnhide = async (review: AdminProductReviewSummary) => {
    setActingId(review.reviewId);
    setError(null);
    setMessage(null);
    try {
      await unhideAdminReview(accessToken, review.reviewId);
      setMessage(`리뷰 #${review.reviewId}을(를) 공개로 복구했습니다.`);
      await refreshCurrentPage();
    } catch (e) {
      setError(e instanceof Error ? e.message : '공개 복구에 실패했습니다.');
    } finally {
      setActingId(null);
    }
  };

  const confirmHide = async () => {
    if (!hideTarget) {
      return;
    }
    setActingId(hideTarget.reviewId);
    setError(null);
    setMessage(null);
    try {
      await hideAdminReview(accessToken, hideTarget.reviewId, hideReason);
      setMessage(`리뷰 #${hideTarget.reviewId}을(를) 비공개 처리했습니다.`);
      setHideTarget(null);
      setHideReason('');
      await refreshCurrentPage();
    } catch (e) {
      setError(e instanceof Error ? e.message : '비공개 처리에 실패했습니다.');
    } finally {
      setActingId(null);
    }
  };

  const pageNumbers = useMemo(() => {
    const total = pageData.totalPages;
    const windowSize = 5;
    const start = Math.max(1, Math.min(currentPage - 2, total - windowSize + 1));
    const end = Math.min(total, start + windowSize - 1);
    const list: number[] = [];
    for (let i = start; i <= end; i += 1) {
      list.push(i);
    }
    return list;
  }, [currentPage, pageData.totalPages]);
  const pageStartRow = pageData.totalElements === 0 ? 0 : (currentPage - 1) * pageData.size + 1;
  const pageEndRow = Math.min(currentPage * pageData.size, pageData.totalElements);

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
        <div className={shellStyles.denied}>관리자만 리뷰 관리 화면을 사용할 수 있습니다.</div>
      </main>
    );
  }

  return (
    <AdminMarketingShell
      currentPath="/admin/reviews"
      title="리뷰관리"
      description=""
      statusText={
        error
          ? error
          : loading
            ? '리뷰 목록을 불러오는 중입니다.'
            : message
              ? message
              : null
      }
      classNames={{
        toolbarCard: shellToolbar.toolbarCard,
        header: shellToolbar.header,
        title: shellToolbar.title,
        subtitle: shellToolbar.subtitle,
        statusText: shellToolbar.statusText,
        stats: shellToolbar.stats,
        statCard: shellToolbar.statCard,
        statLabel: shellToolbar.statLabel,
        statValue: shellToolbar.statValue,
      }}
      stats={[
        { label: '검색 결과', value: pageData.totalElements },
        { label: '현재 페이지', value: `${currentPage} / ${pageData.totalPages}` },
        { label: '필터', value: VISIBILITY_OPTIONS.find(o => o.value === visibility)?.label ?? '전체' },
      ]}
    >
      <section className={styles.panel}>
        <div className={styles.panelHeader}>
          <h2 className={styles.panelTitle}>리뷰 목록</h2>
        </div>
        <div className={styles.panelBody}>
          <div className={styles.toolbar}>
            <form className={styles.searchForm} onSubmit={handleSearchSubmit}>
              <input
                type="search"
                className={styles.searchInput}
                value={keywordInput}
                onChange={event => setKeywordInput(event.target.value)}
                placeholder="리뷰 내용 또는 상품명 검색"
              />
              <select
                className={styles.select}
                value={visibility}
                onChange={event => {
                  setCurrentPage(1);
                  setVisibility(event.target.value as AdminReviewVisibility);
                }}
              >
                {VISIBILITY_OPTIONS.map(option => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
              <button type="submit" className={styles.searchButton} disabled={loading}>
                검색
              </button>
              <button
                type="button"
                className={styles.secondaryButton}
                onClick={handleReset}
                disabled={loading}
              >
                초기화
              </button>
              <button
                type="button"
                className={styles.secondaryButton}
                onClick={() => void refreshCurrentPage()}
                disabled={loading}
              >
                새로고침
              </button>
            </form>
          </div>

          {loading ? (
            <div className={styles.empty}>리뷰 목록을 불러오는 중입니다.</div>
          ) : pageData.reviews.length === 0 ? (
            <div className={styles.empty}>조건에 맞는 리뷰가 없습니다.</div>
          ) : (
            <div className={styles.list}>
              {pageData.reviews.map(review => (
                <article key={review.reviewId} className={styles.card}>
                  <div className={styles.cardHeader}>
                    <div className={styles.titleRow}>
                      <h3 className={styles.productName}>{review.productName}</h3>
                      <p className={styles.metaLine}>
                        리뷰 #{review.reviewId} · 상품 #{review.productId} · 작성자 user #{review.userId}{' '}
                        · {formatCreatedAt(review.createdAt)}
                      </p>
                    </div>
                    <div className={styles.chips}>
                      <span className={styles.chip} aria-label="평점">
                        <span className={styles.rating}>{ratingStars(review.rating)}</span> {review.rating}/5
                      </span>
                      <span className={styles.chip}>도움 {review.helpfulCount}</span>
                      {review.hidden ? (
                        <span className={`${styles.chip} ${styles.chipDanger}`}>비공개</span>
                      ) : (
                        <span className={styles.chip}>공개</span>
                      )}
                    </div>
                  </div>
                  {review.hidden && review.hiddenReason ? (
                    <p className={styles.metaLine}>사유: {review.hiddenReason}</p>
                  ) : null}
                  <p className={styles.content}>{review.content}</p>
                  {review.images.length > 0 ? (
                    <div className={styles.thumbs}>
                      {review.images.map(img => (
                        <a
                          key={img.id}
                          className={styles.thumb}
                          href={img.url}
                          target="_blank"
                          rel="noreferrer"
                          title={img.originalFilename}
                        >
                          <img src={img.url} alt="" />
                        </a>
                      ))}
                    </div>
                  ) : null}
                  <div className={styles.actions}>
                    {review.hidden ? (
                      <button
                        type="button"
                        className={`${styles.actionPrimary} ${styles.actionSuccess}`}
                        disabled={actingId === review.reviewId}
                        onClick={() => void handleUnhide(review)}
                      >
                        공개 복구
                      </button>
                    ) : (
                      <button
                        type="button"
                        className={`${styles.actionPrimary} ${styles.actionDanger}`}
                        disabled={actingId === review.reviewId}
                        onClick={() => {
                          setHideTarget(review);
                          setHideReason('');
                        }}
                      >
                        비공개 처리
                      </button>
                    )}
                  </div>
                </article>
              ))}
            </div>
          )}

          {pageData.totalPages > 1 ? (
            <div className={styles.pagination}>
              <button
                type="button"
                className={styles.pageBtn}
                aria-label="첫 페이지"
                title="첫 페이지"
                disabled={currentPage <= 1 || loading}
                onClick={() => setCurrentPage(1)}
              >
                «
              </button>
              <button
                type="button"
                className={styles.pageBtn}
                disabled={currentPage <= 1 || loading}
                aria-label="이전 페이지"
                title="이전 페이지"
                onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
              >
                ‹
              </button>
              {pageNumbers.map(num => (
                <button
                  key={num}
                  type="button"
                  className={`${styles.pageBtn} ${num === currentPage ? styles.pageBtnActive : ''}`}
                  disabled={loading}
                  onClick={() => setCurrentPage(num)}
                >
                  {num}
                </button>
              ))}
              <button
                type="button"
                className={styles.pageBtn}
                disabled={currentPage >= pageData.totalPages || loading}
                aria-label="다음 페이지"
                title="다음 페이지"
                onClick={() => setCurrentPage(p => p + 1)}
              >
                ›
              </button>
              <button
                type="button"
                className={styles.pageBtn}
                aria-label="마지막 페이지"
                title="마지막 페이지"
                disabled={currentPage >= pageData.totalPages || loading}
                onClick={() => setCurrentPage(pageData.totalPages)}
              >
                »
              </button>
            </div>
          ) : null}
          <p className={styles.paginationInfo} aria-live="polite">
            전체 <strong>{pageData.totalElements}</strong>건 · 페이지당 <strong>{pageData.size}</strong>건 ·{' '}
            <strong>
              {pageStartRow}-{pageEndRow}
            </strong>
            번째 표시 · <strong>{currentPage}</strong> / <strong>{pageData.totalPages}</strong> 페이지
          </p>
        </div>
      </section>

      {hideTarget ? (
        <div
          className={styles.modalOverlay}
          role="presentation"
          onClick={event => {
            if (event.target === event.currentTarget) {
              setHideTarget(null);
            }
          }}
        >
          <div className={styles.modal} role="dialog" aria-modal="true" aria-labelledby="hide-review-title">
            <h3 id="hide-review-title">리뷰 비공개</h3>
            <p>
              상품 «{hideTarget.productName}» 리뷰 #{hideTarget.reviewId}을(를) 비공개로 전환합니다. 사유는 최대 80자까지
              저장됩니다.
            </p>
            <textarea
              className={styles.textarea}
              value={hideReason}
              onChange={event => setHideReason(event.target.value)}
              placeholder="비공개 사유 (선택)"
              maxLength={80}
            />
            <div className={styles.modalActions}>
              <button
                type="button"
                className={styles.secondaryButton}
                onClick={() => setHideTarget(null)}
                disabled={actingId !== null}
              >
                취소
              </button>
              <button
                type="button"
                className={styles.searchButton}
                onClick={() => void confirmHide()}
                disabled={actingId !== null}
              >
                비공개 적용
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </AdminMarketingShell>
  );
}

export default AdminReviewsPage;
