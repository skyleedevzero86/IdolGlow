import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import {
  deleteAdminProduct,
  downloadAdminProductsExcel,
  fetchAdminProduct,
  fetchAdminProducts,
  type AdminProductSummary,
  type ProductTourAttractionItem,
} from '../../../shared/data/adminBookingApi';
import { formatTourSpotLabel } from '../../../shared/data/seoulTourDistrictOptions';
import { AdminMarketingShell } from '../AdminMarketingPage/AdminMarketingShell';
import shellStyles from '../AdminMarketingPage/AdminMarketingPage.module.css';
import shellToolbar from '../AdminUsersPage/AdminUsersShellToolbar.module.css';
import styles from '../AdminBookingManagement/AdminBookingManagement.module.css';

const PAGE_SIZE = 10;
const PAGE_WINDOW = 10;

function formatCurrency(value: number | null | undefined): string {
  const n = Number(value);
  if (!Number.isFinite(n)) {
    return '—';
  }
  return new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency: 'KRW',
    maximumFractionDigits: 0,
  }).format(n);
}

function hasProductMapLocation(item: AdminProductSummary): boolean {
  const loc = item.location;
  if (!loc) {
    return false;
  }
  const addr = typeof loc.displayAddress === 'string' ? loc.displayAddress.trim() : '';
  if (addr && addr !== '—') {
    return true;
  }
  return Boolean(String(loc.name ?? '').trim());
}

function productKakaoMapUrl(item: AdminProductSummary): string | null {
  const loc = item.location;
  if (!loc) {
    return null;
  }
  const lat = typeof loc.latitude === 'number' ? loc.latitude : Number(loc.latitude);
  const lng = typeof loc.longitude === 'number' ? loc.longitude : Number(loc.longitude);
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) {
    return null;
  }
  const name = String(loc.name ?? '').trim() || '위치';
  return `https://map.kakao.com/link/map/${encodeURIComponent(name)},${lat},${lng}`;
}

export function AdminProductsPage() {
  const navigate = useNavigate();
  const { accessToken, authReady, user } = useAuth();
  const [items, setItems] = useState<readonly AdminProductSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [keywordInput, setKeywordInput] = useState('');
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(1);
  const [downloading, setDownloading] = useState(false);
  const [tourPicksModal, setTourPicksModal] = useState<{
    readonly productName: string;
    readonly picks: readonly ProductTourAttractionItem[];
  } | null>(null);
  const [tourPicksModalLoading, setTourPicksModalLoading] = useState(false);
  const [tourPicksModalError, setTourPicksModalError] = useState<string | null>(null);

  useEffect(() => {
    if (!authReady || user?.role !== 'ADMIN') {
      return;
    }

    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await fetchAdminProducts(accessToken, keyword);
        setItems(response);
      } catch (loadError) {
        setError(loadError instanceof Error ? loadError.message : '상품 목록을 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };

    void load();
  }, [accessToken, authReady, keyword, user?.role]);

  const pagedItems = useMemo(
    () => items.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE),
    [items, page]
  );

  const totalPages = Math.max(1, Math.ceil(items.length / PAGE_SIZE));
  const pageNumbers = useMemo(() => {
    const start = Math.floor((page - 1) / PAGE_WINDOW) * PAGE_WINDOW + 1;
    const end = Math.min(totalPages, start + PAGE_WINDOW - 1);
    return Array.from({ length: end - start + 1 }, (_, index) => start + index);
  }, [page, totalPages]);
  const pageStartRow = items.length === 0 ? 0 : (page - 1) * PAGE_SIZE + 1;
  const pageEndRow = Math.min(page * PAGE_SIZE, items.length);

  useEffect(() => {
    if (page > totalPages) {
      setPage(totalPages);
    }
  }, [page, totalPages]);

  useEffect(() => {
    if (!tourPicksModal && !tourPicksModalError) {
      return;
    }
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setTourPicksModal(null);
        setTourPicksModalError(null);
      }
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [tourPicksModal, tourPicksModalError]);

  const slotCount = useMemo(
    () => items.reduce((sum, item) => sum + (item.description ? 1 : 0), 0),
    [items]
  );

  const mapOrLinkedTourCount = useMemo(
    () =>
      items.filter(item => hasProductMapLocation(item) || item.tourAttractionPickCount > 0).length,
    [items],
  );

  const openTourPicksModal = useCallback(
    async (productId: number, productName: string) => {
      setTourPicksModal(null);
      setTourPicksModalError(null);
      setTourPicksModalLoading(true);
      try {
        const detail = await fetchAdminProduct(accessToken, productId);
        const picks = detail.tourAttractionPicks ?? [];
        setTourPicksModal({ productName, picks });
      } catch (loadError) {
        setTourPicksModalError(
          loadError instanceof Error ? loadError.message : '연계 관광지 정보를 불러오지 못했습니다.',
        );
      } finally {
        setTourPicksModalLoading(false);
      }
    },
    [accessToken],
  );

  const handleDelete = async (productId: number, productName: string) => {
    if (!window.confirm(`${productName} 상품을 삭제하시겠습니까?`)) {
      return;
    }

    try {
      await deleteAdminProduct(accessToken, productId);
      const response = await fetchAdminProducts(accessToken, keyword);
      setItems(response);
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : '상품을 삭제하지 못했습니다.');
    }
  };

  const handleDownloadExcel = async () => {
    setDownloading(true);
    setError(null);
    try {
      await downloadAdminProductsExcel(accessToken);
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
        <div className={shellStyles.denied}>관리자만 상품관리 화면을 사용할 수 있습니다.</div>
      </main>
    );
  }

  return (
    <AdminMarketingShell
      currentPath="/admin/products"
      title="상품관리"
      description=""
      headerAside={
        <button
          type="button"
          className={styles.secondaryButton}
          onClick={() => navigate('/glow_map')}
        >
          Glow 지도
        </button>
      }
      classNames={{
        toolbarCard: shellToolbar.toolbarCard,
        header: shellToolbar.header,
        title: shellToolbar.title,
        statusText: shellToolbar.statusText,
      }}
      statusText={error ?? (loading ? '상품 목록을 불러오는 중입니다.' : null)}
      stats={[]}
    >
      <section className={styles.section}>
        <div className={styles.statGrid}>
          <article className={styles.statCard}>
            <span className={styles.statLabel}>등록 상품</span>
            <strong className={styles.statValue}>{items.length}</strong>
          </article>
          <article className={styles.statCard}>
            <span className={styles.statLabel}>지도 위치 · 연계 관광지</span>
            <strong className={styles.statValue}>{mapOrLinkedTourCount}</strong>
          </article>
          <article className={styles.statCard}>
            <span className={styles.statLabel}>태그 사용 상품</span>
            <strong className={styles.statValue}>
              {items.filter(item => item.tagNames.length > 0).length}
            </strong>
          </article>
          <article className={styles.statCard}>
            <span className={styles.statLabel}>설명 작성 상품</span>
            <strong className={styles.statValue}>{slotCount}</strong>
          </article>
        </div>

        <section className={styles.panel}>
          <div className={styles.panelBody}>
            <div className={styles.toolbar}>
              <div className={styles.toolbarLeft}>
                <input
                  className={styles.searchInput}
                  value={keywordInput}
                  onChange={event => setKeywordInput(event.target.value)}
                  placeholder="상품명 검색"
                />
                <button
                  type="button"
                  className={styles.secondaryButton}
                  onClick={() => {
                    setPage(1);
                    setKeyword(keywordInput.trim());
                  }}
                >
                  검색
                </button>
                <button
                  type="button"
                  className={styles.ghostButton}
                  onClick={() => {
                    setKeywordInput('');
                    setKeyword('');
                    setPage(1);
                  }}
                >
                  초기화
                </button>
              </div>

              <div className={styles.toolbarRight}>
                <p className={styles.summary}>전체 {items.length}건</p>
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
                  onClick={() => navigate('/admin/products/new')}
                >
                  상품 등록
                </button>
              </div>
            </div>
          </div>
        </section>

        <section className={styles.panel}>
          <div className={styles.tableWrap}>
            <table className={styles.table}>
              <colgroup>
                <col className={styles.cIdx} />
                <col className={styles.cName} />
                <col className={styles.cMoney} />
                <col className={styles.cMoney} />
                <col className={styles.cMoney} />
                <col className={styles.cTag} />
                <col className={styles.cLoc} />
                <col className={styles.cAct} />
              </colgroup>
              <thead>
                <tr>
                  <th>번호</th>
                  <th className={styles.left}>상품명</th>
                  <th className={styles.num}>상품가</th>
                  <th className={styles.num}>옵션합</th>
                  <th className={styles.num}>총(전체)</th>
                  <th>태그</th>
                  <th className={styles.left}>위치</th>
                  <th>관리</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={8}>불러오는 중입니다.</td>
                  </tr>
                ) : pagedItems.length === 0 ? (
                  <tr>
                    <td colSpan={8}>등록된 상품이 없습니다.</td>
                  </tr>
                ) : (
                  pagedItems.map((item, index) => {
                    const kakaoMapUrl = productKakaoMapUrl(item);
                    const addressText = item.location?.displayAddress?.trim() ?? '';
                    const showAddress = addressText.length > 0 && addressText !== '—';
                    return (
                    <tr key={item.id}>
                      <td>{(page - 1) * PAGE_SIZE + index + 1}</td>
                      <td className={styles.left}>{item.name}</td>
                      <td className={styles.num}>{formatCurrency(item.basePrice)}</td>
                      <td className={styles.num}>{formatCurrency(item.optionsTotalPrice)}</td>
                      <td className={styles.num}>{formatCurrency(item.totalPrice)}</td>
                      <td>{item.tagNames.length ? item.tagNames.join(', ') : '-'}</td>
                      <td className={styles.left}>
                        <div className={styles.locationCell}>
                          {showAddress ? <span>{addressText}</span> : null}
                          {kakaoMapUrl ? (
                            <a
                              href={kakaoMapUrl}
                              className={styles.tourPicksOpenButton}
                              target="_blank"
                              rel="noopener noreferrer"
                            >
                              지도
                            </a>
                          ) : null}
                          <button
                            type="button"
                            className={styles.tourPicksOpenButton}
                            onClick={() => void openTourPicksModal(item.id, item.name)}
                          >
                            미리보기
                          </button>
                        </div>
                      </td>
                      <td>
                        <div className={styles.buttonRow}>
                          <button
                            type="button"
                            className={styles.secondaryButton}
                            onClick={() => navigate(`/admin/products/${item.id}/edit`)}
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
                    );
                  })
                )}
              </tbody>
            </table>
          </div>

          <div className={styles.paginationWrap}>
            <p className={styles.paginationInfo} aria-live="polite">
              전체 <strong>{items.length}</strong>건 · 페이지당 <strong>{PAGE_SIZE}</strong>건 ·{' '}
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

      {tourPicksModalLoading ? (
        <div
          className={styles.modalBackdrop}
          role="dialog"
          aria-modal="true"
          aria-labelledby="tour-picks-modal-title"
          aria-busy="true"
        >
          <div className={styles.modalPanel}>
            <h2 id="tour-picks-modal-title" className={styles.modalTitle}>
              상품 기준 주변/연계 관광지 추천
            </h2>
            <p className={styles.modalSubtitle}>불러오는 중입니다.</p>
          </div>
        </div>
      ) : null}

      {tourPicksModalError && !tourPicksModal ? (
        <div
          className={styles.modalBackdrop}
          role="alertdialog"
          aria-modal="true"
          aria-labelledby="tour-picks-error-title"
          onClick={() => setTourPicksModalError(null)}
        >
          <div className={styles.modalPanel} onClick={event => event.stopPropagation()}>
            <h2 id="tour-picks-error-title" className={styles.modalTitle}>
              연계 관광지
            </h2>
            <p className={styles.modalSubtitle}>{tourPicksModalError}</p>
            <div className={styles.modalFooter}>
              <button type="button" className={styles.secondaryButton} onClick={() => setTourPicksModalError(null)}>
                닫기
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {tourPicksModal ? (
        <div className={styles.modalBackdrop} role="presentation" onClick={() => setTourPicksModal(null)}>
          <div
            className={styles.modalPanel}
            role="dialog"
            aria-modal="true"
            aria-labelledby="tour-picks-list-title"
            tabIndex={-1}
            onClick={event => event.stopPropagation()}
          >
            <h2 id="tour-picks-list-title" className={styles.modalTitle}>
              상품 기준 주변/연계 관광지 추천 (미리보기)
            </h2>
            <p className={styles.modalSubtitle}>
              {tourPicksModal.productName} · 저장된 선택 {tourPicksModal.picks.length}곳
            </p>
            {tourPicksModal.picks.length === 0 ? (
              <p className={styles.modalSubtitle}>
                상품 편집에서 저장한 연계 관광지가 없습니다. 편집 화면에서 조회·선택 후 저장해 주세요.
              </p>
            ) : (
              <ul className={styles.modalList}>
                {tourPicksModal.picks.map(pick => (
                  <li key={pick.attractionCode} className={styles.modalListItem}>
                    {formatTourSpotLabel(pick)}
                    {(pick.categoryLarge ?? pick.categoryMiddle) ? (
                      <span className={styles.modalListMeta}>
                        {[pick.categoryLarge, pick.categoryMiddle].filter(Boolean).join(' · ')}
                      </span>
                    ) : null}
                  </li>
                ))}
              </ul>
            )}
            <div className={styles.modalFooter}>
              <button type="button" className={styles.primaryButton} onClick={() => setTourPicksModal(null)}>
                닫기
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </AdminMarketingShell>
  );
}

export default AdminProductsPage;
