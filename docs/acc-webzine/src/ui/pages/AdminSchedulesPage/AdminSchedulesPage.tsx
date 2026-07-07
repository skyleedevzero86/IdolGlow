import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import MarkdownEditorField from '../../components/MarkdownEditorField/MarkdownEditorField';
import {
  createAdminSlots,
  downloadAdminSlotsExcel,
  fetchAdminProducts,
  fetchAdminSlots,
  type AdminProductSummary,
  type AdminSlotSummary,
} from '../../../shared/data/adminBookingApi';
import { AdminMarketingShell } from '../AdminMarketingPage/AdminMarketingShell';
import shellStyles from '../AdminMarketingPage/AdminMarketingPage.module.css';
import shellToolbar from '../AdminUsersPage/AdminUsersShellToolbar.module.css';
import styles from '../AdminBookingManagement/AdminBookingManagement.module.css';

const PAGE_SIZE = 10;
const PAGE_WINDOW = 10;

type ScheduleFormState = {
  readonly productId: number | null;
  readonly startDate: string;
  readonly endDate: string;
  readonly startHour: number;
  readonly endHour: number;
  readonly excludeWeekends: boolean;
  readonly adminNote: string;
};

const EMPTY_FORM: ScheduleFormState = {
  productId: null,
  startDate: '',
  endDate: '',
  startHour: 9,
  endHour: 16,
  excludeWeekends: false,
  adminNote: '',
};

export function AdminSchedulesPage() {
  const { accessToken, authReady, user } = useAuth();
  const [searchParams] = useSearchParams();
  const [products, setProducts] = useState<readonly AdminProductSummary[]>([]);
  const [form, setForm] = useState<ScheduleFormState>(EMPTY_FORM);
  const [slots, setSlots] = useState<readonly AdminSlotSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [page, setPage] = useState(1);
  const [downloading, setDownloading] = useState(false);

  useEffect(() => {
    if (!authReady || user?.role !== 'ADMIN') {
      return;
    }

    const loadProducts = async () => {
      setLoading(true);
      setError(null);
      try {
        const productItems = await fetchAdminProducts(accessToken);
        setProducts(productItems);
        const queryProductId = Number(searchParams.get('productId'));
        const initialProductId =
          productItems.find(item => item.id === queryProductId)?.id ?? productItems[0]?.id ?? null;
        setForm(previous => ({ ...previous, productId: initialProductId }));
      } catch (loadError) {
        setError(loadError instanceof Error ? loadError.message : '상품 목록을 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };

    void loadProducts();
  }, [accessToken, authReady, searchParams, user?.role]);

  useEffect(() => {
    if (!form.productId) {
      return;
    }

    const productId = form.productId;

    const loadSlots = async () => {
      try {
        const response = await fetchAdminSlots(accessToken, productId);
        setSlots(response);
      } catch (loadError) {
        setError(loadError instanceof Error ? loadError.message : '일정 목록을 불러오지 못했습니다.');
      }
    };

    void loadSlots();
  }, [accessToken, form.productId]);

  const filteredSlots = useMemo(
    () =>
      slots.filter(slot => {
        if (form.startDate && slot.reservationDate < form.startDate) {
          return false;
        }
        if (form.endDate && slot.reservationDate > form.endDate) {
          return false;
        }
        return true;
      }),
    [form.endDate, form.startDate, slots]
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

  const handleCreate = async () => {
    if (!form.productId || !form.startDate || !form.endDate) {
      setError('상품, 시작일, 종료일을 입력해 주세요.');
      return;
    }

    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      const response = await createAdminSlots(accessToken, form.productId, {
        startDate: form.startDate,
        endDate: form.endDate,
        startHour: form.startHour,
        endHour: form.endHour,
        excludeWeekends: form.excludeWeekends,
        adminNote: form.adminNote,
      });
      setSlots(response);
      setMessage('일정을 생성했습니다.');
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : '일정을 생성하지 못했습니다.');
    } finally {
      setSaving(false);
    }
  };

  const handleDownloadExcel = async () => {
    if (!form.productId) {
      return;
    }
    setDownloading(true);
    setError(null);
    try {
      await downloadAdminSlotsExcel(accessToken, {
        productId: form.productId,
        dateFrom: form.startDate || undefined,
        dateTo: form.endDate || undefined,
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
        <div className={shellStyles.denied}>관리자만 일정관리 화면을 사용할 수 있습니다.</div>
      </main>
    );
  }

  return (
    <AdminMarketingShell
      currentPath="/admin/schedules"
      title="일정관리"
      description=""
      classNames={{
        toolbarCard: shellToolbar.toolbarCard,
        header: shellToolbar.header,
        title: shellToolbar.title,
        statusText: shellToolbar.statusText,
      }}
      statusText={error ?? message}
      stats={[]}
    >
      <section className={styles.section}>
        <section className={styles.panel}>
          <div className={styles.panelBody}>
            <div className={styles.formGrid}>
              <label className={styles.field}>
                <span className={styles.label}>상품 선택</span>
                <select
                  className={styles.select}
                  value={form.productId ?? ''}
                  onChange={event =>
                    setForm(previous => ({
                      ...previous,
                      productId: Number(event.target.value),
                    }))
                  }
                >
                  {products.map(product => (
                    <option key={product.id} value={product.id}>
                      {product.name}
                    </option>
                  ))}
                </select>
              </label>

              <label className={styles.field}>
                <span className={styles.label}>주말 제외</span>
                <select
                  className={styles.select}
                  value={form.excludeWeekends ? 'Y' : 'N'}
                  onChange={event =>
                    setForm(previous => ({
                      ...previous,
                      excludeWeekends: event.target.value === 'Y',
                    }))
                  }
                >
                  <option value="N">포함</option>
                  <option value="Y">제외</option>
                </select>
              </label>

              <label className={styles.field}>
                <span className={styles.label}>시작일</span>
                <input
                  className={styles.dateInput}
                  type="date"
                  value={form.startDate}
                  onChange={event =>
                    setForm(previous => ({ ...previous, startDate: event.target.value }))
                  }
                />
              </label>

              <label className={styles.field}>
                <span className={styles.label}>종료일</span>
                <input
                  className={styles.dateInput}
                  type="date"
                  value={form.endDate}
                  onChange={event =>
                    setForm(previous => ({ ...previous, endDate: event.target.value }))
                  }
                />
              </label>

              <label className={styles.field}>
                <span className={styles.label}>시작 시간</span>
                <input
                  className={styles.numberInput}
                  type="number"
                  min={9}
                  max={15}
                  value={form.startHour}
                  onChange={event =>
                    setForm(previous => ({
                      ...previous,
                      startHour: Number(event.target.value),
                    }))
                  }
                />
              </label>

              <label className={styles.field}>
                <span className={styles.label}>종료 시간</span>
                <input
                  className={styles.numberInput}
                  type="number"
                  min={10}
                  max={16}
                  value={form.endHour}
                  onChange={event =>
                    setForm(previous => ({
                      ...previous,
                      endHour: Number(event.target.value),
                    }))
                  }
                />
              </label>
            </div>
          </div>
        </section>

        <section className={styles.panel}>
          <div className={styles.panelBody}>
            <MarkdownEditorField
              label="일정 메모"
              value={form.adminNote}
              onChange={value => setForm(previous => ({ ...previous, adminNote: value }))}
              placeholder="이번 일정 묶음에 적용할 메모를 작성해 주세요."
              minHeight={240}
            />
            <div className={styles.buttonRow}>
              <button
                type="button"
                className={styles.secondaryButton}
                onClick={() => void handleDownloadExcel()}
                disabled={loading || downloading || !form.productId}
              >
                엑셀 다운로드
              </button>
              <button
                type="button"
                className={styles.primaryButton}
                onClick={() => void handleCreate()}
                disabled={saving || loading}
              >
                {saving ? '생성 중...' : '일정 생성'}
              </button>
            </div>
          </div>
        </section>

        <div className={styles.statGrid}>
          <article className={styles.statCard}>
            <span className={styles.statLabel}>현재 상품 일정</span>
            <strong className={styles.statValue}>{slots.length}</strong>
          </article>
          <article className={styles.statCard}>
            <span className={styles.statLabel}>조회 구간 일정</span>
            <strong className={styles.statValue}>{filteredSlots.length}</strong>
          </article>
          <article className={styles.statCard}>
            <span className={styles.statLabel}>예약 완료 일정</span>
            <strong className={styles.statValue}>{filteredSlots.filter(slot => slot.booked).length}</strong>
          </article>
          <article className={styles.statCard}>
            <span className={styles.statLabel}>메모 일정</span>
            <strong className={styles.statValue}>
              {filteredSlots.filter(slot => slot.adminNote?.trim()).length}
            </strong>
          </article>
        </div>

        <section className={styles.panel}>
          <div className={styles.tableWrap}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>날짜</th>
                  <th>시간</th>
                  <th>예약 상태</th>
                  <th>메모</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={4}>불러오는 중입니다.</td>
                  </tr>
                ) : pagedSlots.length === 0 ? (
                  <tr>
                    <td colSpan={4}>등록된 일정이 없습니다.</td>
                  </tr>
                ) : (
                  pagedSlots.map(slot => (
                    <tr key={slot.id}>
                      <td>{slot.reservationDate}</td>
                      <td>{slot.startTime.slice(0, 5)} - {slot.endTime.slice(0, 5)}</td>
                      <td>{slot.booked ? '예약완료' : slot.holdReservationId ? '홀드중' : '사용가능'}</td>
                      <td>{slot.adminNote?.trim() ? '작성' : '-'}</td>
                    </tr>
                  ))
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
      </section>
    </AdminMarketingShell>
  );
}

export default AdminSchedulesPage;
