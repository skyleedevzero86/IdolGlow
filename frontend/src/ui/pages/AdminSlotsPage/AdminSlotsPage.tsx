import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import MarkdownEditorField from '../../components/MarkdownEditorField/MarkdownEditorField';
import {
  deleteAdminSlot,
  downloadAdminSlotsExcel,
  fetchAdminProducts,
  fetchAdminSlots,
  updateAdminSlotNote,
  type AdminProductSummary,
  type AdminSlotSummary,
} from '../../../shared/data/adminBookingApi';
import { AdminMarketingShell } from '../AdminMarketingPage/AdminMarketingShell';
import shellStyles from '../AdminMarketingPage/AdminMarketingPage.module.css';
import shellToolbar from '../AdminUsersPage/AdminUsersShellToolbar.module.css';
import styles from '../AdminBookingManagement/AdminBookingManagement.module.css';

const PAGE_SIZE = 10;
const PAGE_WINDOW = 10;

function formatStatus(slot: AdminSlotSummary) {
  if (slot.booked) {
    return { label: '예약완료', className: styles.statusDanger };
  }
  if (slot.holdReservationId) {
    return { label: '홀드중', className: styles.statusWarning };
  }
  return { label: '사용가능', className: styles.statusSuccess };
}

export function AdminSlotsPage() {
  const navigate = useNavigate();
  const { accessToken, authReady, user } = useAuth();
  const [products, setProducts] = useState<readonly AdminProductSummary[]>([]);
  const [selectedProductId, setSelectedProductId] = useState<number | null>(null);
  const [slots, setSlots] = useState<readonly AdminSlotSummary[]>([]);
  const [selectedSlotId, setSelectedSlotId] = useState<number | null>(null);
  const [editorValue, setEditorValue] = useState('');
  const [dateFilter, setDateFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'AVAILABLE' | 'BOOKED' | 'HOLD'>('ALL');
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(1);
  const [downloading, setDownloading] = useState(false);

  useEffect(() => {
    if (!authReady || user?.role !== 'ADMIN') {
      return;
    }

    const loadProducts = async () => {
      try {
        const productItems = await fetchAdminProducts(accessToken);
        setProducts(productItems);
      } catch (loadError) {
        setError(loadError instanceof Error ? loadError.message : '상품 목록을 불러오지 못했습니다.');
      }
    };

    void loadProducts();
  }, [accessToken, authReady, user?.role]);

  useEffect(() => {
    if (!selectedProductId) {
      return;
    }

    const productId = selectedProductId;

    const loadSlots = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await fetchAdminSlots(accessToken, productId);
        setSlots(response);
        setSelectedSlotId(previous => {
          const nextSelected = response.find(slot => slot.id === previous) ?? response[0] ?? null;
          setEditorValue(nextSelected?.adminNote ?? '');
          return nextSelected?.id ?? null;
        });
      } catch (loadError) {
        setError(loadError instanceof Error ? loadError.message : '룸 목록을 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };

    void loadSlots();
  }, [accessToken, selectedProductId]);

  const selectedSlot = useMemo(
    () => slots.find(slot => slot.id === selectedSlotId) ?? null,
    [selectedSlotId, slots]
  );

  const filteredSlots = useMemo(
    () =>
      slots.filter(slot => {
        if (dateFilter && slot.reservationDate !== dateFilter) {
          return false;
        }
        if (statusFilter === 'BOOKED' && !slot.booked) {
          return false;
        }
        if (statusFilter === 'HOLD' && !slot.holdReservationId) {
          return false;
        }
        if (statusFilter === 'AVAILABLE' && (slot.booked || slot.holdReservationId)) {
          return false;
        }
        return true;
      }),
    [dateFilter, slots, statusFilter]
  );

  const pagedSlots = useMemo(
    () => filteredSlots.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE),
    [filteredSlots, page]
  );

  const totalPages = Math.max(1, Math.ceil(filteredSlots.length / PAGE_SIZE));
  const pageNumbers = useMemo(() => {
    const start = Math.floor((page - 1) / PAGE_WINDOW) * PAGE_WINDOW + 1;
    const end = Math.min(totalPages, start + PAGE_WINDOW - 1);
    return Array.from({ length: end - start + 1 }, (_, index) => start + index);
  }, [page, totalPages]);
  const pageStartRow = filteredSlots.length === 0 ? 0 : (page - 1) * PAGE_SIZE + 1;
  const pageEndRow = Math.min(page * PAGE_SIZE, filteredSlots.length);

  useEffect(() => {
    if (page > totalPages) {
      setPage(totalPages);
    }
  }, [page, totalPages]);

  const handleSave = async () => {
    if (!selectedSlot) {
      return;
    }

    setSaving(true);
    setError(null);
    try {
      const updated = await updateAdminSlotNote(accessToken, selectedSlot.id, editorValue);
      setSlots(previous =>
        previous.map(item => (item.id === updated.id ? updated : item))
      );
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : '메모를 저장하지 못했습니다.');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (slotId: number) => {
    if (!window.confirm('선택한 룸 일정을 삭제하시겠습니까?')) {
      return;
    }

    try {
      await deleteAdminSlot(accessToken, slotId);
      if (selectedProductId) {
        const response = await fetchAdminSlots(accessToken, selectedProductId);
        setSlots(response);
        const nextSelected = response[0] ?? null;
        setSelectedSlotId(nextSelected?.id ?? null);
        setEditorValue(nextSelected?.adminNote ?? '');
      }
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : '룸 일정을 삭제하지 못했습니다.');
    }
  };

  const handleDownloadExcel = async () => {
    if (!selectedProductId) {
      return;
    }
    setDownloading(true);
    setError(null);
    try {
      await downloadAdminSlotsExcel(accessToken, {
        productId: selectedProductId,
        dateFrom: dateFilter || undefined,
        dateTo: dateFilter || undefined,
      });
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
        <div className={shellStyles.denied}>관리자만 룸관리 화면을 사용할 수 있습니다.</div>
      </main>
    );
  }

  return (
    <AdminMarketingShell
      currentPath="/admin/slots"
      title="룸관리"
      description=""
      classNames={{
        toolbarCard: shellToolbar.toolbarCard,
        header: shellToolbar.header,
        title: shellToolbar.title,
        statusText: shellToolbar.statusText,
      }}
      statusText={error ?? (loading ? '룸 목록을 불러오는 중입니다.' : null)}
      stats={[]}
    >
      <section className={styles.section}>
        <div className={styles.statGrid}>
          <article className={styles.statCard}>
            <span className={styles.statLabel}>전체 룸 일정</span>
            <strong className={styles.statValue}>{slots.length}</strong>
          </article>
          <article className={styles.statCard}>
            <span className={styles.statLabel}>사용 가능</span>
            <strong className={styles.statValue}>
              {slots.filter(slot => !slot.booked && !slot.holdReservationId).length}
            </strong>
          </article>
          <article className={styles.statCard}>
            <span className={styles.statLabel}>예약 완료</span>
            <strong className={styles.statValue}>{slots.filter(slot => slot.booked).length}</strong>
          </article>
          <article className={styles.statCard}>
            <span className={styles.statLabel}>메모 작성</span>
            <strong className={styles.statValue}>
              {slots.filter(slot => slot.adminNote?.trim()).length}
            </strong>
          </article>
        </div>

        <section className={styles.panel}>
          <div className={styles.panelBody}>
            <div className={styles.toolbar}>
              <div className={styles.toolbarLeft}>
                <select
                  className={styles.select}
                  value={selectedProductId ?? ''}
                  aria-label="상품 선택"
                  disabled={loading || products.length === 0}
                  onChange={event => {
                    const raw = event.target.value;
                    setSelectedProductId(raw === '' ? null : Number(raw));
                    setPage(1);
                  }}
                >
                  <option value="" disabled>
                    {products.length === 0 ? '등록된 상품이 없습니다.' : '상품을 선택해 주세요'}
                  </option>
                  {products.map(product => (
                    <option key={product.id} value={product.id}>
                      {product.name}
                    </option>
                  ))}
                </select>
                <input
                  className={styles.dateInput}
                  type="date"
                  value={dateFilter}
                  onChange={event => {
                    setPage(1);
                    setDateFilter(event.target.value);
                  }}
                />
                <select
                  className={styles.select}
                  value={statusFilter}
                  onChange={event => {
                    setPage(1);
                    setStatusFilter(event.target.value as 'ALL' | 'AVAILABLE' | 'BOOKED' | 'HOLD');
                  }}
                >
                  <option value="ALL">전체 상태</option>
                  <option value="AVAILABLE">사용가능</option>
                  <option value="BOOKED">예약완료</option>
                  <option value="HOLD">홀드중</option>
                </select>
              </div>
              <div className={styles.toolbarRight}>
                <button
                  type="button"
                  className={styles.secondaryButton}
                  onClick={() => void handleDownloadExcel()}
                  disabled={!selectedProductId || downloading}
                >
                  엑셀 다운로드
                </button>
                <button
                  type="button"
                  className={styles.primaryButton}
                  onClick={() =>
                    navigate(
                      selectedProductId
                        ? `/admin/schedules?productId=${selectedProductId}`
                        : '/admin/schedules'
                    )
                  }
                >
                  일정 등록
                </button>
              </div>
            </div>
          </div>
        </section>

        <section className={styles.grid}>
          <section className={styles.panel}>
            <div className={styles.tableWrap}>
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th>날짜</th>
                    <th>시간</th>
                    <th>상태</th>
                    <th>홀드</th>
                    <th>메모</th>
                    <th>관리</th>
                  </tr>
                </thead>
                <tbody>
                  {loading ? (
                    <tr>
                      <td colSpan={6}>불러오는 중입니다.</td>
                    </tr>
                  ) : !selectedProductId ? (
                    <tr>
                      <td colSpan={6}>상품을 선택하면 룸 일정이 표시됩니다.</td>
                    </tr>
                  ) : pagedSlots.length === 0 ? (
                    <tr>
                      <td colSpan={6}>조건에 맞는 룸 일정이 없습니다.</td>
                    </tr>
                  ) : (
                    pagedSlots.map(slot => {
                      const status = formatStatus(slot);
                      return (
                        <tr
                          key={slot.id}
                          className={`${styles.rowButton} ${selectedSlotId === slot.id ? styles.activeRow : ''}`}
                          onClick={() => {
                            setSelectedSlotId(slot.id);
                            setEditorValue(slot.adminNote ?? '');
                          }}
                        >
                          <td>{slot.reservationDate}</td>
                          <td>{slot.startTime.slice(0, 5)} - {slot.endTime.slice(0, 5)}</td>
                          <td>
                            <span className={`${styles.statusChip} ${status.className}`}>
                              {status.label}
                            </span>
                          </td>
                          <td>{slot.holdReservationId ? `#${slot.holdReservationId}` : '-'}</td>
                          <td>{slot.adminNote?.trim() ? '작성' : '-'}</td>
                          <td>
                            <button
                              type="button"
                              className={styles.dangerButton}
                              onClick={event => {
                                event.stopPropagation();
                                void handleDelete(slot.id);
                              }}
                            >
                              삭제
                            </button>
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
                전체 <strong>{filteredSlots.length}</strong>건 · 페이지당 <strong>{PAGE_SIZE}</strong>건 ·{' '}
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

          <section className={styles.detailPanel}>
            <div className={styles.detailBody}>
              {selectedSlot ? (
                <>
                  <h2 className={styles.detailTitle}>{selectedSlot.productName}</h2>
                  <div className={styles.detailMeta}>
                    <div className={styles.metaItem}>
                      <span className={styles.metaLabel}>예약일</span>
                      <strong className={styles.metaValue}>{selectedSlot.reservationDate}</strong>
                    </div>
                    <div className={styles.metaItem}>
                      <span className={styles.metaLabel}>시간</span>
                      <strong className={styles.metaValue}>
                        {selectedSlot.startTime.slice(0, 5)} - {selectedSlot.endTime.slice(0, 5)}
                      </strong>
                    </div>
                  </div>

                  <MarkdownEditorField
                    label="룸 메모"
                    value={editorValue}
                    onChange={setEditorValue}
                    placeholder="룸 운영 메모를 마크다운으로 작성해 주세요."
                    minHeight={240}
                  />

                  <div className={styles.buttonRow}>
                    <button
                      type="button"
                      className={styles.primaryButton}
                      onClick={() => void handleSave()}
                      disabled={saving}
                    >
                      {saving ? '저장 중...' : '메모 저장'}
                    </button>
                  </div>
                </>
              ) : (
                <div className={styles.empty}>선택된 룸 일정이 없습니다.</div>
              )}
            </div>
          </section>
        </section>
      </section>
    </AdminMarketingShell>
  );
}

export default AdminSlotsPage;
