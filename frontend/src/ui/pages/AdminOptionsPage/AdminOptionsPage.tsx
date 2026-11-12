import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import {
  deleteAdminOption,
  downloadAdminOptionsExcel,
  fetchAdminOptions,
  type AdminOptionSummary,
} from '../../../shared/data/adminBookingApi';
import { AdminMarketingShell } from '../AdminMarketingPage/AdminMarketingShell';
import shellStyles from '../AdminMarketingPage/AdminMarketingPage.module.css';
import shellToolbar from '../AdminUsersPage/AdminUsersShellToolbar.module.css';
import styles from '../AdminBookingManagement/AdminBookingManagement.module.css';

const PAGE_SIZE = 10;
const PAGE_WINDOW = 10;

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency: 'KRW',
    maximumFractionDigits: 0,
  }).format(value);
}

export function AdminOptionsPage() {
  const navigate = useNavigate();
  const { accessToken, authReady, user } = useAuth();
  const [items, setItems] = useState<readonly AdminOptionSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(1);
  const [downloading, setDownloading] = useState(false);

  useEffect(() => {
    if (!authReady || user?.role !== 'ADMIN') {
      return;
    }

    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await fetchAdminOptions(accessToken);
        setItems(response);
      } catch (loadError) {
        setError(loadError instanceof Error ? loadError.message : '옵션 목록을 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };

    void load();
  }, [accessToken, authReady, user?.role]);

  const filteredItems = useMemo(
    () =>
      items.filter(
        item =>
          !keyword.trim() ||
          item.name.toLowerCase().includes(keyword.toLowerCase()) ||
          item.location.toLowerCase().includes(keyword.toLowerCase())
      ),
    [items, keyword]
  );

  const pagedItems = useMemo(
    () => filteredItems.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE),
    [filteredItems, page]
  );

  const totalPages = Math.max(1, Math.ceil(filteredItems.length / PAGE_SIZE));
  const pageNumbers = useMemo(() => {
    const start = Math.floor((page - 1) / PAGE_WINDOW) * PAGE_WINDOW + 1;
    const end = Math.min(totalPages, start + PAGE_WINDOW - 1);
    return Array.from({ length: end - start + 1 }, (_, index) => start + index);
  }, [page, totalPages]);
  const pageStartRow = filteredItems.length === 0 ? 0 : (page - 1) * PAGE_SIZE + 1;
  const pageEndRow = Math.min(page * PAGE_SIZE, filteredItems.length);

  useEffect(() => {
    if (page > totalPages) {
      setPage(totalPages);
    }
  }, [page, totalPages]);

  const handleDelete = async (optionId: number, optionName: string) => {
    if (!window.confirm(`${optionName} 옵션을 삭제하시겠습니까?`)) {
      return;
    }

    try {
      await deleteAdminOption(accessToken, optionId);
      const response = await fetchAdminOptions(accessToken);
      setItems(response);
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : '옵션을 삭제하지 못했습니다.');
    }
  };

  const handleDownloadExcel = async () => {
    setDownloading(true);
    setError(null);
    try {
      await downloadAdminOptionsExcel(accessToken);
    } catch (downloadError) {
      setError(downloadError instanceof Error ? downloadError.message : '엑셀 다운로드에 실패했습니다.');
    } finally {
      setDownloading(false);
    }
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
        <div className={shellStyles.denied}>관리자만 옵션관리 화면을 사용할 수 있습니다.</div>
      </main>
    );
  }

  return (
    <AdminMarketingShell
      currentPath="/admin/options"
      title="옵션관리"
      description=""
      classNames={{
        toolbarCard: shellToolbar.toolbarCard,
        header: shellToolbar.header,
        title: shellToolbar.title,
        statusText: shellToolbar.statusText,
      }}
      statusText={error ?? (loading ? '옵션 목록을 불러오는 중입니다.' : null)}
      stats={[]}
    >
      <section className={styles.section}>
        <div className={styles.statGrid}>
          <article className={styles.statCard}>
            <span className={styles.statLabel}>등록 옵션</span>
            <strong className={styles.statValue}>{items.length}</strong>
          </article>
          <article className={styles.statCard}>
            <span className={styles.statLabel}>검색 결과</span>
            <strong className={styles.statValue}>{filteredItems.length}</strong>
          </article>
          <article className={styles.statCard}>
            <span className={styles.statLabel}>장소 등록 옵션</span>
            <strong className={styles.statValue}>
              {items.filter(item => item.location.trim().length > 0).length}
            </strong>
          </article>
          <article className={styles.statCard}>
            <span className={styles.statLabel}>설명 작성 옵션</span>
            <strong className={styles.statValue}>
              {items.filter(item => item.description.trim().length > 0).length}
            </strong>
          </article>
        </div>

        <section className={styles.panel}>
          <div className={styles.panelBody}>
            <div className={styles.toolbar}>
              <div className={styles.toolbarLeft}>
                <input
                  className={styles.searchInput}
                  value={keyword}
                  onChange={event => {
                    setPage(1);
                    setKeyword(event.target.value);
                  }}
                  placeholder="옵션명 또는 장소 검색"
                />
              </div>
              <div className={styles.toolbarRight}>
                <p className={styles.summary}>전체 {filteredItems.length}건</p>
                <button
                  type="button"
                  className={styles.secondaryButton}
                  onClick={() => void handleDownloadExcel()}
                  disabled={downloading}
                >
                  엑셀 다운로드
                </button>
                <button
                  type="button"
                  className={styles.primaryButton}
                  onClick={() => navigate('/admin/options/new')}
                >
                  옵션 등록
                </button>
              </div>
            </div>
          </div>
        </section>

        <section className={styles.panel}>
          <div className={styles.tableWrap}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>번호</th>
                  <th className={styles.left}>옵션명</th>
                  <th>금액</th>
                  <th className={styles.left}>장소</th>
                  <th>관리</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={5}>불러오는 중입니다.</td>
                  </tr>
                ) : pagedItems.length === 0 ? (
                  <tr>
                    <td colSpan={5}>등록된 옵션이 없습니다.</td>
                  </tr>
                ) : (
                  pagedItems.map((item, index) => (
                    <tr key={item.id}>
                      <td>{(page - 1) * PAGE_SIZE + index + 1}</td>
                      <td className={styles.left}>{item.name}</td>
                      <td>{formatCurrency(item.price)}</td>
                      <td className={styles.left}>{item.location}</td>
                      <td>
                        <div className={styles.buttonRow}>
                          <button
                            type="button"
                            className={styles.secondaryButton}
                            onClick={() => navigate(`/admin/options/${item.id}/edit`)}
                          >
                            수정
                          </button>
                          <button
                            type="button"
                            className={styles.dangerButton}
                            onClick={() => void handleDelete(item.id, item.name)}
                          >
                            삭제
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          <div className={styles.paginationWrap}>
            <p className={styles.paginationInfo} aria-live="polite">
              전체 <strong>{filteredItems.length}</strong>건 · 페이지당 <strong>{PAGE_SIZE}</strong>건 ·{' '}
              <strong>
                {pageStartRow}-{pageEndRow}
              </strong>
              번째 표시 · <strong>{page}</strong> / <strong>{totalPages}</strong> 페이지
            </p>
            <div className={styles.pagination}>
              <button
                type="button"
                className={styles.pageArrowButton}
                onClick={() => setPage(1)}
                disabled={page <= 1}
                aria-label="첫 페이지"
                title="첫 페이지"
              >
                «
              </button>
              <button
                type="button"
                className={styles.pageArrowButton}
                onClick={() => setPage(previous => Math.max(1, previous - 1))}
                disabled={page <= 1}
                aria-label="이전 페이지"
                title="이전 페이지"
              >
                ‹
              </button>
              <div className={styles.paginationNumbers}>
                {pageNumbers.map(number => (
                  <button
                    key={number}
                    type="button"
                    className={`${styles.pageButton} ${number === page ? styles.pageActive : ''}`}
                    onClick={() => setPage(number)}
                    aria-current={number === page ? 'page' : undefined}
                  >
                    {number}
                  </button>
                ))}
              </div>
              <button
                type="button"
                className={styles.pageArrowButton}
                onClick={() => setPage(previous => Math.min(totalPages, previous + 1))}
                disabled={page >= totalPages}
                aria-label="다음 페이지"
                title="다음 페이지"
              >
                ›
              </button>
              <button
                type="button"
                className={styles.pageArrowButton}
                onClick={() => setPage(totalPages)}
                disabled={page >= totalPages}
                aria-label="마지막 페이지"
                title="마지막 페이지"
              >
                »
              </button>
            </div>
          </div>
        </section>
      </section>
    </AdminMarketingShell>
  );
}

export default AdminOptionsPage;
