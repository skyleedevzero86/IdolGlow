import { useEffect, useMemo, useState } from "react";
import { useAuth } from "../../../auth/AuthContext";
import {
  type AdminMenuStats,
  type AdminProductSummary,
  type AdminReservationSummary,
  type ReservationDashboardResponse,
  type ReservationStatus,
  cancelAdminReservation,
  downloadAdminReservationsExcel,
  fetchAdminMenuStats,
  fetchAdminProducts,
  fetchAdminReservationDashboard,
  fetchAdminReservations,
  updateAdminReservationMemo,
} from "../../../shared/data/adminBookingApi";
import MarkdownEditorField from "../../components/MarkdownEditorField/MarkdownEditorField";
import styles from "../AdminBookingManagement/AdminBookingManagement.module.css";
import shellStyles from "../AdminMarketingPage/AdminMarketingPage.module.css";
import { AdminMarketingShell } from "../AdminMarketingPage/AdminMarketingShell";
import shellToolbar from "../AdminUsersPage/AdminUsersShellToolbar.module.css";

const PAGE_SIZE = 10;
const PAGE_WINDOW = 10;

const EMPTY_DASHBOARD: ReservationDashboardResponse = {
  pendingCount: 0,
  bookedCount: 0,
  completedCount: 0,
  canceledCount: 0,
  paymentPendingCount: 0,
  paymentSucceededCount: 0,
  paymentFailedCount: 0,
  paymentCanceledCount: 0,
  paymentExpiredCount: 0,
  recentReservations: [],
};

const EMPTY_MENU_STATS: AdminMenuStats = {
  productsCount: 0,
  optionsCount: 0,
  slotsCount: 0,
  reservationsPendingCount: 0,
  reservationsBookedCount: 0,
  reservationsCompletedCount: 0,
  reservationsCanceledCount: 0,
  paymentsPendingCount: 0,
  paymentsSucceededCount: 0,
  paymentsFailedCount: 0,
  paymentsCanceledCount: 0,
  paymentsExpiredCount: 0,
  paymentsRefundedCount: 0,
  paymentsPartialCanceledCount: 0,
};

const STATUS_OPTIONS: ReadonlyArray<{
  readonly value: ReservationStatus | "";
  readonly label: string;
}> = [
  { value: "", label: "전체 상태" },
  { value: "PENDING", label: "대기" },
  { value: "BOOKED", label: "예약완료" },
  { value: "COMPLETED", label: "완료" },
  { value: "CANCELED", label: "취소" },
];

function formatStatus(status: ReservationStatus) {
  switch (status) {
    case "BOOKED":
    case "COMPLETED":
      return {
        label: status === "BOOKED" ? "예약완료" : "완료",
        className: styles.statusSuccess,
      };
    case "PENDING":
      return { label: "대기", className: styles.statusWarning };
    case "CANCELED":
      return { label: "취소", className: styles.statusDanger };
    default:
      return { label: status, className: styles.statusNeutral };
  }
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat("ko-KR", {
    style: "currency",
    currency: "KRW",
    maximumFractionDigits: 0,
  }).format(value);
}

export function AdminReservationsPage() {
  const { accessToken, authReady, user } = useAuth();
  const [products, setProducts] = useState<readonly AdminProductSummary[]>([]);
  const [dashboard, setDashboard] =
    useState<ReservationDashboardResponse>(EMPTY_DASHBOARD);
  const [menuStats, setMenuStats] = useState<AdminMenuStats>(EMPTY_MENU_STATS);
  const [items, setItems] = useState<readonly AdminReservationSummary[]>([]);
  const [selectedReservationId, setSelectedReservationId] = useState<
    number | null
  >(null);
  const [memoValue, setMemoValue] = useState("");
  const [statusFilter, setStatusFilter] = useState<ReservationStatus | "">("");
  const [productFilter, setProductFilter] = useState<number | "">("");
  const [visitDate, setVisitDate] = useState("");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(1);

  useEffect(() => {
    if (!authReady || user?.role !== "ADMIN") {
      return;
    }

    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const [
          productItems,
          menuStatsResponse,
          dashboardResponse,
          reservationItems,
        ] = await Promise.all([
          fetchAdminProducts(accessToken),
          fetchAdminMenuStats(accessToken),
          fetchAdminReservationDashboard(accessToken, {
            fromDate: visitDate || undefined,
            toDate: visitDate || undefined,
          }),
          fetchAdminReservations(accessToken, {
            status: statusFilter,
            visitDate,
            productId: productFilter,
          }),
        ]);

        setProducts(productItems);
        setMenuStats(menuStatsResponse);
        setDashboard(dashboardResponse);
        setItems(reservationItems);
        setSelectedReservationId((previous) => {
          const nextSelected =
            reservationItems.find((item) => item.reservationId === previous) ??
            reservationItems[0] ??
            null;
          setMemoValue(nextSelected?.adminMemo ?? "");
          return nextSelected?.reservationId ?? null;
        });
      } catch (loadError) {
        setError(
          loadError instanceof Error
            ? loadError.message
            : "예약 목록을 불러오지 못했습니다.",
        );
      } finally {
        setLoading(false);
      }
    };

    void load();
  }, [
    accessToken,
    authReady,
    productFilter,
    statusFilter,
    user?.role,
    visitDate,
  ]);

  const selectedReservation = useMemo(
    () =>
      items.find((item) => item.reservationId === selectedReservationId) ??
      null,
    [items, selectedReservationId],
  );
  const pagedItems = useMemo(
    () => items.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE),
    [items, page],
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
    setPage(1);
  }, [statusFilter, productFilter, visitDate]);

  const handleSaveMemo = async () => {
    if (!selectedReservation) {
      return;
    }

    setSaving(true);
    setError(null);
    try {
      const updated = await updateAdminReservationMemo(
        accessToken,
        selectedReservation.reservationId,
        memoValue,
      );
      setItems((previous) =>
        previous.map((item) =>
          item.reservationId === updated.reservationId ? updated : item,
        ),
      );
    } catch (saveError) {
      setError(
        saveError instanceof Error
          ? saveError.message
          : "예약 메모를 저장하지 못했습니다.",
      );
    } finally {
      setSaving(false);
    }
  };

  const handleCancelReservation = async () => {
    if (
      !selectedReservation ||
      !window.confirm("선택한 예약을 취소하시겠습니까?")
    ) {
      return;
    }

    setSaving(true);
    setError(null);
    try {
      const updated = await cancelAdminReservation(
        accessToken,
        selectedReservation.reservationId,
      );
      setItems((previous) =>
        previous.map((item) =>
          item.reservationId === updated.reservationId ? updated : item,
        ),
      );
    } catch (cancelError) {
      setError(
        cancelError instanceof Error
          ? cancelError.message
          : "예약을 취소하지 못했습니다.",
      );
    } finally {
      setSaving(false);
    }
  };

  const handleDownloadExcel = async () => {
    setDownloading(true);
    setError(null);
    try {
      await downloadAdminReservationsExcel(accessToken, { visitDate });
    } catch (downloadError) {
      setError(
        downloadError instanceof Error
          ? downloadError.message
          : "엑셀 다운로드에 실패했습니다.",
      );
    } finally {
      setDownloading(false);
    }
  };

  if (!authReady) {
    return (
      <main className={shellStyles.page}>
        <div className={shellStyles.loading}>
          관리자 권한을 확인하는 중입니다.
        </div>
      </main>
    );
  }

  if (user?.role !== "ADMIN") {
    return (
      <main className={shellStyles.page}>
        <div className={shellStyles.denied}>
          관리자만 예약관리 화면을 사용할 수 있습니다.
        </div>
      </main>
    );
  }

  return (
    <AdminMarketingShell
      currentPath="/admin/reservations"
      title="예약관리"
      description=""
      classNames={{
        toolbarCard: shellToolbar.toolbarCard,
        header: shellToolbar.header,
        title: shellToolbar.title,
        statusText: shellToolbar.statusText,
      }}
      statusText={error ?? (loading ? "예약 목록을 불러오는 중입니다." : null)}
      stats={[
        { label: "상품", value: String(menuStats.productsCount) },
        { label: "옵션", value: String(menuStats.optionsCount) },
        { label: "슬롯", value: String(menuStats.slotsCount) },
      ]}
    >
      <section className={styles.section}>
        <div className={styles.statGrid}>
          <article className={styles.statCard}>
            <span className={styles.statLabel}>예약 대기</span>
            <strong className={styles.statValue}>
              {dashboard.pendingCount}
            </strong>
          </article>
          <article className={styles.statCard}>
            <span className={styles.statLabel}>예약 완료</span>
            <strong className={styles.statValue}>
              {dashboard.bookedCount}
            </strong>
          </article>
          <article className={styles.statCard}>
            <span className={styles.statLabel}>방문 완료</span>
            <strong className={styles.statValue}>
              {dashboard.completedCount}
            </strong>
          </article>
          <article className={styles.statCard}>
            <span className={styles.statLabel}>취소 예약</span>
            <strong className={styles.statValue}>
              {dashboard.canceledCount}
            </strong>
          </article>
        </div>

        <section className={styles.panel}>
          <div className={styles.panelBody}>
            <div className={styles.toolbar}>
              <div className={styles.toolbarLeft}>
                <select
                  className={styles.select}
                  value={statusFilter}
                  onChange={(event) =>
                    setStatusFilter(
                      event.target.value as ReservationStatus | "",
                    )
                  }
                >
                  {STATUS_OPTIONS.map((option) => (
                    <option key={option.label} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
                <select
                  className={styles.select}
                  value={productFilter}
                  onChange={(event) =>
                    setProductFilter(
                      event.target.value ? Number(event.target.value) : "",
                    )
                  }
                >
                  <option value="">전체 상품</option>
                  {products.map((product) => (
                    <option key={product.id} value={product.id}>
                      {product.name}
                    </option>
                  ))}
                </select>
                <input
                  className={styles.dateInput}
                  type="date"
                  value={visitDate}
                  onChange={(event) => setVisitDate(event.target.value)}
                />
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
                    <th>예약번호</th>
                    <th className={styles.left}>상품명</th>
                    <th>방문일</th>
                    <th>시간</th>
                    <th>상태</th>
                    <th>결제상태</th>
                    <th>금액</th>
                  </tr>
                </thead>
                <tbody>
                  {loading ? (
                    <tr>
                      <td colSpan={7}>불러오는 중입니다.</td>
                    </tr>
                  ) : pagedItems.length === 0 ? (
                    <tr>
                      <td colSpan={7}>예약 내역이 없습니다.</td>
                    </tr>
                  ) : (
                    pagedItems.map((item) => {
                      const status = formatStatus(item.status);
                      return (
                        <tr
                          key={item.reservationId}
                          className={`${styles.rowButton} ${selectedReservationId === item.reservationId ? styles.activeRow : ""}`}
                          onClick={() => {
                            setSelectedReservationId(item.reservationId);
                            setMemoValue(item.adminMemo ?? "");
                          }}
                        >
                          <td>{item.reservationId}</td>
                          <td className={styles.left}>{item.productName}</td>
                          <td>{item.visitDate}</td>
                          <td>
                            {item.visitStartTime.slice(0, 5)} -{" "}
                            {item.visitEndTime.slice(0, 5)}
                          </td>
                          <td>
                            <span
                              className={`${styles.statusChip} ${status.className}`}
                            >
                              {status.label}
                            </span>
                          </td>
                          <td>{item.paymentStatus ?? "-"}</td>
                          <td>{formatCurrency(item.totalPrice)}</td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            <div className={styles.paginationWrap}>
              <p className={styles.paginationInfo} aria-live="polite">
                전체 <strong>{items.length}</strong>건 · 페이지당 <strong>{PAGE_SIZE}</strong>건 ·{" "}
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
                  onClick={() => setPage((previous) => Math.max(1, previous - 1))}
                  disabled={page <= 1}
                  aria-label="이전 페이지"
                  title="이전 페이지"
                >
                  ‹
                </button>
                <div className={styles.paginationNumbers}>
                  {pageNumbers.map((number) => (
                    <button
                      key={number}
                      type="button"
                      className={`${styles.pageButton} ${number === page ? styles.pageActive : ""}`}
                      onClick={() => setPage(number)}
                      aria-current={number === page ? "page" : undefined}
                    >
                      {number}
                    </button>
                  ))}
                </div>
                <button
                  type="button"
                  className={styles.pageArrowButton}
                  onClick={() => setPage((previous) => Math.min(totalPages, previous + 1))}
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
            </div>
          </section>

          <section className={styles.detailPanel}>
            <div className={styles.detailBody}>
              {selectedReservation ? (
                <>
                  <h2 className={styles.detailTitle}>
                    예약 #{selectedReservation.reservationId}
                  </h2>
                  <div className={styles.detailMeta}>
                    <div className={styles.metaItem}>
                      <span className={styles.metaLabel}>상품</span>
                      <strong className={styles.metaValue}>
                        {selectedReservation.productName}
                      </strong>
                    </div>
                    <div className={styles.metaItem}>
                      <span className={styles.metaLabel}>회원</span>
                      <strong className={styles.metaValue}>
                        #{selectedReservation.userId}
                      </strong>
                    </div>
                    <div className={styles.metaItem}>
                      <span className={styles.metaLabel}>방문</span>
                      <strong className={styles.metaValue}>
                        {selectedReservation.visitDate}{" "}
                        {selectedReservation.visitStartTime.slice(0, 5)} -{" "}
                        {selectedReservation.visitEndTime.slice(0, 5)}
                      </strong>
                    </div>
                    <div className={styles.metaItem}>
                      <span className={styles.metaLabel}>결제상태</span>
                      <strong className={styles.metaValue}>
                        {selectedReservation.paymentStatus ?? "-"}
                      </strong>
                    </div>
                  </div>

                  <MarkdownEditorField
                    label="예약 메모"
                    value={memoValue}
                    onChange={setMemoValue}
                    placeholder="예약 처리 메모를 마크다운으로 작성해 주세요."
                    minHeight={220}
                  />

                  <div className={styles.buttonRow}>
                    <button
                      type="button"
                      className={styles.primaryButton}
                      onClick={() => void handleSaveMemo()}
                      disabled={saving}
                    >
                      {saving ? "저장 중..." : "메모 저장"}
                    </button>
                    <button
                      type="button"
                      className={styles.dangerButton}
                      onClick={() => void handleCancelReservation()}
                      disabled={
                        saving || selectedReservation.status === "CANCELED"
                      }
                    >
                      예약 취소
                    </button>
                  </div>
                </>
              ) : (
                <div className={styles.empty}>선택된 예약이 없습니다.</div>
              )}
            </div>
          </section>
        </section>
      </section>
    </AdminMarketingShell>
  );
}

export default AdminReservationsPage;
