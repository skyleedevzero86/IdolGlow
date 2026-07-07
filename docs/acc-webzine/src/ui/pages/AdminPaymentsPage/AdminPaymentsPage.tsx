import { useEffect, useMemo, useState } from "react";
import { useAuth } from "../../../auth/AuthContext";
import {
  type AdminMenuStats,
  type AdminPaymentCharts,
  type AdminPaymentDetail,
  type AdminPaymentOverview,
  type AdminPaymentRefundSummary,
  type AdminPaymentSummary,
  type AdminProductSummary,
  type PaymentStatus,
  cancelAdminPayment,
  downloadAdminPaymentReceiptPdf,
  downloadAdminPaymentsExcel,
  fetchAdminMenuStats,
  fetchAdminPaymentCharts,
  fetchAdminPaymentDetail,
  fetchAdminPaymentOverview,
  fetchAdminPayments,
  fetchAdminProducts,
  retryAdminPaymentRefund,
} from "../../../shared/data/adminBookingApi";
import styles from "../AdminBookingManagement/AdminBookingManagement.module.css";
import shellStyles from "../AdminMarketingPage/AdminMarketingPage.module.css";
import { AdminMarketingShell } from "../AdminMarketingPage/AdminMarketingShell";
import shellToolbar from "../AdminUsersPage/AdminUsersShellToolbar.module.css";

const PAGE_SIZE = 10;
const PAGE_WINDOW = 10;

const STATUS_OPTIONS: ReadonlyArray<{
  readonly value: PaymentStatus | "";
  readonly label: string;
}> = [
  { value: "", label: "전체 상태" },
  { value: "PENDING", label: "대기" },
  { value: "SUCCEEDED", label: "성공" },
  { value: "FAILED", label: "실패" },
  { value: "CANCELED", label: "취소" },
  { value: "EXPIRED", label: "만료" },
  { value: "REFUNDED", label: "환불" },
  { value: "PARTIAL_CANCELED", label: "부분취소" },
];

const EMPTY_OVERVIEW: AdminPaymentOverview = {
  totalCount: 0,
  pendingCount: 0,
  succeededCount: 0,
  failedCount: 0,
  canceledCount: 0,
  expiredCount: 0,
  refundedCount: 0,
  partialCanceledCount: 0,
  cancelableCount: 0,
  grossAmount: 0,
  refundedAmount: 0,
  netAmount: 0,
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

const EMPTY_CHARTS: AdminPaymentCharts = {
  byStatus: [],
  byMonth: [],
};

function formatCurrency(value: number): string {
  return new Intl.NumberFormat("ko-KR", {
    style: "currency",
    currency: "KRW",
    maximumFractionDigits: 0,
  }).format(value);
}

function paymentStatusLabel(status: PaymentStatus): string {
  switch (status) {
    case "PENDING":
      return "대기";
    case "SUCCEEDED":
      return "성공";
    case "FAILED":
      return "실패";
    case "CANCELED":
      return "취소";
    case "EXPIRED":
      return "만료";
    case "REFUNDED":
      return "환불";
    case "PARTIAL_CANCELED":
      return "부분취소";
    default:
      return status;
  }
}

function paymentStatusClass(status: PaymentStatus): string {
  switch (status) {
    case "SUCCEEDED":
      return styles.statusSuccess;
    case "REFUNDED":
    case "PARTIAL_CANCELED":
      return styles.statusWarning;
    case "FAILED":
    case "CANCELED":
    case "EXPIRED":
      return styles.statusDanger;
    default:
      return styles.statusNeutral;
  }
}

function formatDateTime(value: string | null): string {
  if (!value) {
    return "-";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

export function AdminPaymentsPage() {
  const { accessToken, authReady, user } = useAuth();
  const [products, setProducts] = useState<readonly AdminProductSummary[]>([]);
  const [menuStats, setMenuStats] = useState<AdminMenuStats>(EMPTY_MENU_STATS);
  const [charts, setCharts] = useState<AdminPaymentCharts>(EMPTY_CHARTS);
  const [overview, setOverview] =
    useState<AdminPaymentOverview>(EMPTY_OVERVIEW);
  const [items, setItems] = useState<readonly AdminPaymentSummary[]>([]);
  const [selectedPaymentId, setSelectedPaymentId] = useState<number | null>(
    null,
  );
  const [detail, setDetail] = useState<AdminPaymentDetail | null>(null);
  const [statusFilter, setStatusFilter] = useState<PaymentStatus | "">("");
  const [productFilter, setProductFilter] = useState<number | "">("");
  const [visitDate, setVisitDate] = useState("");
  const [loading, setLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(1);

  const filterParams = useMemo(
    () => ({
      status: statusFilter,
      visitDate,
      productId: productFilter,
    }),
    [productFilter, statusFilter, visitDate],
  );

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
          overviewResponse,
          chartsResponse,
          paymentItems,
        ] = await Promise.all([
          fetchAdminProducts(accessToken),
          fetchAdminMenuStats(accessToken),
          fetchAdminPaymentOverview(accessToken, filterParams),
          fetchAdminPaymentCharts(accessToken, filterParams),
          fetchAdminPayments(accessToken, filterParams),
        ]);
        setProducts(productItems);
        setMenuStats(menuStatsResponse);
        setOverview(overviewResponse);
        setCharts(chartsResponse);
        setItems(paymentItems);
        setSelectedPaymentId(
          (previous) =>
            paymentItems.find((item) => item.paymentId === previous)
              ?.paymentId ??
            paymentItems[0]?.paymentId ??
            null,
        );
      } catch (loadError) {
        setError(
          loadError instanceof Error
            ? loadError.message
            : "결제 목록을 불러오지 못했습니다.",
        );
      } finally {
        setLoading(false);
      }
    };

    void load();
  }, [accessToken, authReady, filterParams, user?.role]);

  useEffect(() => {
    if (!selectedPaymentId) {
      setDetail(null);
      return;
    }

    const load = async () => {
      setDetailLoading(true);
      try {
        const response = await fetchAdminPaymentDetail(
          accessToken,
          selectedPaymentId,
        );
        setDetail(response);
      } catch (loadError) {
        setError(
          loadError instanceof Error
            ? loadError.message
            : "결제 상세를 불러오지 못했습니다.",
        );
      } finally {
        setDetailLoading(false);
      }
    };

    void load();
  }, [accessToken, selectedPaymentId]);

  const latestFailedRefund = useMemo<AdminPaymentRefundSummary | null>(
    () => detail?.refunds.find((refund) => refund.status === "FAILED") ?? null,
    [detail],
  );
  const maxStatusCount = Math.max(
    1,
    ...charts.byStatus.map((item) => item.count),
  );
  const maxMonthTotal = Math.max(
    1,
    ...charts.byMonth.map((item) => item.totalCount),
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

  const handleDownloadExcel = async () => {
    setActionLoading(true);
    setError(null);
    try {
      await downloadAdminPaymentsExcel(accessToken, filterParams);
    } catch (downloadError) {
      setError(
        downloadError instanceof Error
          ? downloadError.message
          : "엑셀 다운로드에 실패했습니다.",
      );
    } finally {
      setActionLoading(false);
    }
  };

  const handleReceipt = async () => {
    if (!detail) {
      return;
    }
    setActionLoading(true);
    setError(null);
    try {
      await downloadAdminPaymentReceiptPdf(accessToken, detail.paymentId);
    } catch (receiptError) {
      setError(
        receiptError instanceof Error
          ? receiptError.message
          : "영수증 PDF 출력에 실패했습니다.",
      );
    } finally {
      setActionLoading(false);
    }
  };

  const handleCancelPayment = async () => {
    if (!detail || !detail.canCancel) {
      return;
    }
    const reason = window.prompt(
      "관리자 취소 사유를 입력해 주세요.",
      "관리자 결제 취소",
    );
    if (reason === null) {
      return;
    }
    setActionLoading(true);
    setError(null);
    try {
      const updated = await cancelAdminPayment(
        accessToken,
        detail.paymentId,
        reason,
      );
      setDetail(updated);
      setItems((previous) =>
        previous.map((item) =>
          item.paymentId === updated.paymentId
            ? {
                ...item,
                status: updated.status,
                cancelAmount: updated.cancelAmount,
                failureReason: updated.failureReason,
                approvedAt: updated.approvedAt,
                failedAt: updated.failedAt,
              }
            : item,
        ),
      );
      const refreshedOverview = await fetchAdminPaymentOverview(
        accessToken,
        filterParams,
      );
      setOverview(refreshedOverview);
    } catch (cancelError) {
      setError(
        cancelError instanceof Error
          ? cancelError.message
          : "관리자 결제 취소에 실패했습니다.",
      );
    } finally {
      setActionLoading(false);
    }
  };

  const handleRetryRefund = async () => {
    if (!detail || !latestFailedRefund) {
      return;
    }
    setActionLoading(true);
    setError(null);
    try {
      await retryAdminPaymentRefund(accessToken, detail.paymentId);
      const refreshed = await fetchAdminPaymentDetail(
        accessToken,
        detail.paymentId,
      );
      setDetail(refreshed);
      const refreshedOverview = await fetchAdminPaymentOverview(
        accessToken,
        filterParams,
      );
      setOverview(refreshedOverview);
    } catch (retryError) {
      setError(
        retryError instanceof Error
          ? retryError.message
          : "환불 재시도에 실패했습니다.",
      );
    } finally {
      setActionLoading(false);
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
          관리자만 결제관리 화면을 사용할 수 있습니다.
        </div>
      </main>
    );
  }

  return (
    <AdminMarketingShell
      currentPath="/admin/payments"
      title="결제관리"
      description=""
      classNames={{
        toolbarCard: shellToolbar.toolbarCard,
        header: shellToolbar.header,
        title: shellToolbar.title,
        statusText: shellToolbar.statusText,
      }}
      statusText={
        error ?? (loading ? "결제 데이터를 불러오는 중입니다." : null)
      }
      stats={[
        { label: "상품", value: String(menuStats.productsCount) },
        { label: "옵션", value: String(menuStats.optionsCount) },
        { label: "슬롯", value: String(menuStats.slotsCount) },
      ]}
    >
      <section className={styles.section}>
        <div className={styles.statGrid}>
          <article className={styles.statCard}>
            <span className={styles.statLabel}>전체 결제</span>
            <strong className={styles.statValue}>{overview.totalCount}</strong>
          </article>
          <article className={styles.statCard}>
            <span className={styles.statLabel}>성공 결제</span>
            <strong className={styles.statValue}>
              {overview.succeededCount}
            </strong>
          </article>
          <article className={styles.statCard}>
            <span className={styles.statLabel}>실패/에러</span>
            <strong className={styles.statValue}>
              {overview.failedCount + overview.expiredCount}
            </strong>
          </article>
          <article className={styles.statCard}>
            <span className={styles.statLabel}>관리자 취소 가능</span>
            <strong className={styles.statValue}>
              {overview.cancelableCount}
            </strong>
          </article>
        </div>

        <div className={styles.statGrid}>
          <article className={styles.statCard}>
            <span className={styles.statLabel}>총 결제금액</span>
            <strong className={styles.statValue}>
              {formatCurrency(overview.grossAmount)}
            </strong>
          </article>
          <article className={styles.statCard}>
            <span className={styles.statLabel}>총 취소금액</span>
            <strong className={styles.statValue}>
              {formatCurrency(overview.refundedAmount)}
            </strong>
          </article>
          <article className={styles.statCard}>
            <span className={styles.statLabel}>실 결제금액</span>
            <strong className={styles.statValue}>
              {formatCurrency(overview.netAmount)}
            </strong>
          </article>
          <article className={styles.statCard}>
            <span className={styles.statLabel}>취소/환불 건수</span>
            <strong className={styles.statValue}>
              {overview.canceledCount +
                overview.refundedCount +
                overview.partialCanceledCount}
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
                    setStatusFilter(event.target.value as PaymentStatus | "")
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
                  disabled={actionLoading}
                >
                  엑셀 다운로드
                </button>
              </div>
            </div>
          </div>
        </section>

        <section className={styles.panel}>
          <div className={styles.panelBody}>
            <div className={styles.grid}>
              <article className={styles.detailPanel}>
                <div className={styles.detailBody}>
                  <h3 className={styles.detailTitle}>상태별 결제 분포</h3>
                  {charts.byStatus.length === 0 ? (
                    <div className={styles.empty}>차트 데이터가 없습니다.</div>
                  ) : (
                    <div className={styles.refundList}>
                      {charts.byStatus.map((point) => (
                        <div key={point.status}>
                          <div className={styles.toolbar}>
                            <span className={styles.metaLabel}>
                              {point.status}
                            </span>
                            <strong className={styles.metaValue}>
                              {point.count}
                            </strong>
                          </div>
                          <div className={styles.tableWrap}>
                            <div
                              style={{
                                height: "10px",
                                width: `${(point.count / maxStatusCount) * 100}%`,
                                minWidth: "8px",
                                background: "#111827",
                              }}
                            />
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </article>
              <article className={styles.detailPanel}>
                <div className={styles.detailBody}>
                  <h3 className={styles.detailTitle}>월별 결제 추이</h3>
                  {charts.byMonth.length === 0 ? (
                    <div className={styles.empty}>차트 데이터가 없습니다.</div>
                  ) : (
                    <div className={styles.refundList}>
                      {charts.byMonth.map((point) => (
                        <div key={point.month}>
                          <div className={styles.toolbar}>
                            <span className={styles.metaLabel}>
                              {point.month}
                            </span>
                            <strong className={styles.metaValue}>
                              총 {point.totalCount}건
                            </strong>
                          </div>
                          <div className={styles.tableWrap}>
                            <div
                              style={{
                                height: "10px",
                                width: `${(point.totalCount / maxMonthTotal) * 100}%`,
                                minWidth: "8px",
                                background: "#2563eb",
                              }}
                            />
                          </div>
                          <p className={styles.helper}>
                            성공 {point.succeededCount} · 실패{" "}
                            {point.failedCount} · 취소 {point.canceledCount}
                          </p>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </article>
            </div>
          </div>
        </section>

        <section className={styles.grid}>
          <section className={styles.panel}>
            <div className={styles.tableWrap}>
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th>결제번호</th>
                    <th className={styles.left}>상품명</th>
                    <th>방문일</th>
                    <th>결제상태</th>
                    <th>결제금액</th>
                    <th>취소금액</th>
                  </tr>
                </thead>
                <tbody>
                  {loading ? (
                    <tr>
                      <td colSpan={6}>불러오는 중입니다.</td>
                    </tr>
                  ) : pagedItems.length === 0 ? (
                    <tr>
                      <td colSpan={6}>결제 이력이 없습니다.</td>
                    </tr>
                  ) : (
                    pagedItems.map((item) => (
                      <tr
                        key={item.paymentId}
                        className={`${styles.rowButton} ${selectedPaymentId === item.paymentId ? styles.activeRow : ""}`}
                        onClick={() => setSelectedPaymentId(item.paymentId)}
                      >
                        <td>{item.paymentId}</td>
                        <td className={styles.left}>{item.productName}</td>
                        <td>{item.visitDate}</td>
                        <td>
                          <span
                            className={`${styles.statusChip} ${paymentStatusClass(item.status)}`}
                          >
                            {paymentStatusLabel(item.status)}
                          </span>
                        </td>
                        <td>{formatCurrency(item.amount)}</td>
                        <td>{formatCurrency(item.cancelAmount)}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
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
          </section>

          <section className={styles.detailPanel}>
            <div className={styles.detailBody}>
              {detailLoading ? (
                <div className={styles.empty}>
                  결제 상세를 불러오는 중입니다.
                </div>
              ) : detail ? (
                <>
                  <h2 className={styles.detailTitle}>
                    결제 #{detail.paymentId}
                  </h2>
                  <div className={styles.detailMeta}>
                    <div className={styles.metaItem}>
                      <span className={styles.metaLabel}>상태</span>
                      <strong className={styles.metaValue}>
                        {paymentStatusLabel(detail.status)}
                      </strong>
                    </div>
                    <div className={styles.metaItem}>
                      <span className={styles.metaLabel}>결제참조</span>
                      <strong className={styles.metaValue}>
                        {detail.paymentReference}
                      </strong>
                    </div>
                    <div className={styles.metaItem}>
                      <span className={styles.metaLabel}>주문번호</span>
                      <strong className={styles.metaValue}>
                        {detail.orderId}
                      </strong>
                    </div>
                    <div className={styles.metaItem}>
                      <span className={styles.metaLabel}>회원</span>
                      <strong className={styles.metaValue}>
                        #{detail.userId}
                      </strong>
                    </div>
                    <div className={styles.metaItem}>
                      <span className={styles.metaLabel}>결제수단</span>
                      <strong className={styles.metaValue}>
                        {detail.gatewayMethod || detail.provider}
                      </strong>
                    </div>
                    <div className={styles.metaItem}>
                      <span className={styles.metaLabel}>방문일시</span>
                      <strong className={styles.metaValue}>
                        {detail.visitDate} {detail.visitStartTime.slice(0, 5)} -{" "}
                        {detail.visitEndTime.slice(0, 5)}
                      </strong>
                    </div>
                    <div className={styles.metaItem}>
                      <span className={styles.metaLabel}>성공시각</span>
                      <strong className={styles.metaValue}>
                        {formatDateTime(detail.approvedAt)}
                      </strong>
                    </div>
                    <div className={styles.metaItem}>
                      <span className={styles.metaLabel}>취소시각</span>
                      <strong className={styles.metaValue}>
                        {formatDateTime(detail.canceledAt)}
                      </strong>
                    </div>
                    <div className={styles.metaItem}>
                      <span className={styles.metaLabel}>실패사유</span>
                      <strong className={styles.metaValue}>
                        {detail.failureReason || "-"}
                      </strong>
                    </div>
                    <div className={styles.metaItem}>
                      <span className={styles.metaLabel}>결제금액</span>
                      <strong className={styles.metaValue}>
                        {formatCurrency(detail.amount)}
                      </strong>
                    </div>
                    <div className={styles.metaItem}>
                      <span className={styles.metaLabel}>취소금액</span>
                      <strong className={styles.metaValue}>
                        {formatCurrency(detail.cancelAmount)}
                      </strong>
                    </div>
                    <div className={styles.metaItem}>
                      <span className={styles.metaLabel}>카드사/간편결제</span>
                      <strong className={styles.metaValue}>
                        {detail.cardCompany || detail.easyPayProvider || "-"}
                      </strong>
                    </div>
                  </div>

                  <div className={styles.buttonRow}>
                    <button
                      type="button"
                      className={styles.secondaryButton}
                      onClick={() => void handleDownloadExcel()}
                      disabled={actionLoading}
                    >
                      엑셀 다운로드
                    </button>
                    <button
                      type="button"
                      className={styles.secondaryButton}
                      onClick={() => void handleReceipt()}
                      disabled={actionLoading || !detail.receiptAvailable}
                    >
                      영수증 PDF
                    </button>
                    <button
                      type="button"
                      className={styles.secondaryButton}
                      onClick={() => void handleRetryRefund()}
                      disabled={actionLoading || latestFailedRefund == null}
                    >
                      환불 재시도
                    </button>
                    <button
                      type="button"
                      className={styles.dangerButton}
                      onClick={() => void handleCancelPayment()}
                      disabled={actionLoading || !detail.canCancel}
                    >
                      관리자 결제 취소
                    </button>
                  </div>

                  <section>
                    <h3 className={styles.detailTitle}>환불 이력</h3>
                    <div className={styles.refundList}>
                      {detail.refunds.length === 0 ? (
                        <div className={styles.empty}>
                          환불 이력이 없습니다.
                        </div>
                      ) : (
                        detail.refunds.map((refund) => (
                          <article
                            key={refund.refundId}
                            className={styles.refundCard}
                          >
                            <h4 className={styles.refundTitle}>
                              환불 #{refund.refundId}
                            </h4>
                            <div className={styles.refundMeta}>
                              <div className={styles.metaItem}>
                                <span className={styles.metaLabel}>상태</span>
                                <strong className={styles.metaValue}>
                                  {refund.status}
                                </strong>
                              </div>
                              <div className={styles.metaItem}>
                                <span className={styles.metaLabel}>금액</span>
                                <strong className={styles.metaValue}>
                                  {formatCurrency(refund.cancelAmount)}
                                </strong>
                              </div>
                              <div className={styles.metaItem}>
                                <span className={styles.metaLabel}>요청자</span>
                                <strong className={styles.metaValue}>
                                  {refund.requestedBy}
                                </strong>
                              </div>
                              <div className={styles.metaItem}>
                                <span className={styles.metaLabel}>사유</span>
                                <strong className={styles.metaValue}>
                                  {refund.cancelReason}
                                </strong>
                              </div>
                            </div>
                          </article>
                        ))
                      )}
                    </div>
                  </section>

                  <section>
                    <h3 className={styles.detailTitle}>백엔드 결제 이력</h3>
                    <div className={styles.refundList}>
                      {detail.logs.length === 0 ? (
                        <div className={styles.empty}>
                          백엔드 로그가 없습니다.
                        </div>
                      ) : (
                        detail.logs.map((log) => (
                          <article
                            key={log.logId}
                            className={styles.refundCard}
                          >
                            <h4 className={styles.refundTitle}>
                              {log.logType} {log.step ? `· ${log.step}` : ""}
                            </h4>
                            <div className={styles.refundMeta}>
                              <div className={styles.metaItem}>
                                <span className={styles.metaLabel}>시각</span>
                                <strong className={styles.metaValue}>
                                  {formatDateTime(log.createdAt)}
                                </strong>
                              </div>
                              <div className={styles.metaItem}>
                                <span className={styles.metaLabel}>HTTP</span>
                                <strong className={styles.metaValue}>
                                  {[log.httpMethod, log.httpStatus]
                                    .filter(Boolean)
                                    .join(" ") || "-"}
                                </strong>
                              </div>
                              <div className={styles.metaItem}>
                                <span className={styles.metaLabel}>경로</span>
                                <strong className={styles.metaValue}>
                                  {log.requestUrl || "-"}
                                </strong>
                              </div>
                              <div className={styles.metaItem}>
                                <span className={styles.metaLabel}>메시지</span>
                                <strong className={styles.metaValue}>
                                  {log.errorMessage || log.errorCode || "-"}
                                </strong>
                              </div>
                            </div>
                          </article>
                        ))
                      )}
                    </div>
                  </section>
                </>
              ) : (
                <div className={styles.empty}>선택된 결제가 없습니다.</div>
              )}
            </div>
          </section>
        </section>
      </section>
    </AdminMarketingShell>
  );
}

export default AdminPaymentsPage;
