import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../../auth/AuthContext";
import {
  type MyPaymentSummary,
  cancelMyPayment,
  downloadMyPaymentReceipt,
  fetchMyPayments,
} from "../../../auth/authApi";
import shellStyles from "../AdminMarketingPage/AdminMarketingPage.module.css";
import { AdminMarketingShell } from "../AdminMarketingPage/AdminMarketingShell";
import plainStyles from "../MyPagePage/MyPagePlain.module.css";

export function MyPaymentsPage() {
  const { accessToken, authReady, user } = useAuth();
  const [payments, setPayments] = useState<readonly MyPaymentSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [actingPaymentId, setActingPaymentId] = useState<number | null>(null);

  useEffect(() => {
    if (!authReady || !accessToken || !user) {
      return;
    }
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await fetchMyPayments(accessToken);
        setPayments(response ?? []);
      } catch (loadError) {
        setError(
          loadError instanceof Error
            ? loadError.message
            : "결제 내역을 불러오지 못했습니다.",
        );
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, [accessToken, authReady, user]);

  const handlePaymentCancel = async (paymentId: number) => {
    if (!accessToken) return;
    const reason = window.prompt(
      "취소 사유를 입력해 주세요.",
      "사용자 결제 취소",
    );
    if (reason === null) return;
    setActingPaymentId(paymentId);
    setError(null);
    try {
      const updated = await cancelMyPayment(accessToken, paymentId, reason);
      if (!updated) throw new Error("결제 취소에 실패했습니다.");
      setPayments((previous) =>
        previous.map((payment) =>
          payment.paymentId === paymentId ? updated : payment,
        ),
      );
    } catch (cancelError) {
      setError(
        cancelError instanceof Error
          ? cancelError.message
          : "결제 취소에 실패했습니다.",
      );
    } finally {
      setActingPaymentId(null);
    }
  };

  const handleReceiptDownload = async (paymentId: number) => {
    if (!accessToken) return;
    setActingPaymentId(paymentId);
    setError(null);
    try {
      await downloadMyPaymentReceipt(accessToken, paymentId);
    } catch (receiptError) {
      setError(
        receiptError instanceof Error
          ? receiptError.message
          : "영수증 출력에 실패했습니다.",
      );
    } finally {
      setActingPaymentId(null);
    }
  };

  const totalAmount = payments.reduce((sum, payment) => sum + payment.amount, 0);
  const cancelableCount = payments.filter((payment) => payment.canCancel).length;

  if (!authReady) {
    return (
      <main className={shellStyles.page}>
        <div className={shellStyles.loading}>
          결제 정보를 불러오는 중입니다.
        </div>
      </main>
    );
  }

  return (
    <AdminMarketingShell
      currentPath="/my-payments"
      title="결제 내역"
      description="최근 결제 상태와 취소 가능 기간을 확인할 수 있습니다."
      statusText={user ? `${user.nickname || user.email}님의 결제 내역` : null}
      classNames={{
        toolbarCard: plainStyles.flatToolbar,
      }}
      stats={[
        { label: "결제 건수", value: loading ? "…" : String(payments.length) },
        {
          label: "총 결제금액",
          value: loading ? "…" : `${new Intl.NumberFormat("ko-KR").format(totalAmount)}원`,
        },
        { label: "취소 가능", value: loading ? "…" : String(cancelableCount) },
      ]}
    >
      <section className={`${shellStyles.panel} ${plainStyles.flatPanel}`}>
        <div className={shellStyles.panelHeader}>
          <h2 className={shellStyles.panelTitle}>결제 관리</h2>
        </div>
        <div className={shellStyles.panelBody}>
          {error ? (
            <p className={plainStyles.userPaymentError}>{error}</p>
          ) : null}
          {loading ? (
            <div className={shellStyles.empty}>
              결제 내역을 불러오는 중입니다.
            </div>
          ) : payments.length === 0 ? (
            <div className={shellStyles.empty}>결제 이력이 없습니다.</div>
          ) : (
            <div className={plainStyles.userPaymentList}>
              {payments.map((payment) => (
                <article
                  key={payment.paymentId}
                  className={plainStyles.userPaymentCard}
                >
                  <div className={plainStyles.userPaymentMeta}>
                    <strong>
                      <Link to={`/products/${payment.productId}`}>
                        {payment.productName}
                      </Link>
                    </strong>
                    <span className={plainStyles.userPaymentDate}>
                      {payment.visitDate}{" "}
                      {payment.visitStartTime.slice(0, 5)} -{" "}
                      {payment.visitEndTime.slice(0, 5)}
                    </span>
                    <span className={plainStyles.userPaymentText}>
                      결제상태: {payment.status}
                    </span>
                    <span className={plainStyles.userPaymentText}>
                      결제금액:{" "}
                      {new Intl.NumberFormat("ko-KR").format(
                        payment.amount,
                      )}
                      원
                    </span>
                    {payment.cancelDeadlineAt ? (
                      <span className={plainStyles.userPaymentText}>
                        사용자 취소 가능 기한:{" "}
                        {payment.cancelDeadlineAt
                          .replace("T", " ")
                          .slice(0, 16)}
                      </span>
                    ) : null}
                    {payment.failureReason ? (
                      <span className={plainStyles.userPaymentText}>
                        실패사유: {payment.failureReason}
                      </span>
                    ) : null}
                  </div>
                  <div className={plainStyles.userPaymentActions}>
                    <button
                      type="button"
                      className={shellStyles.secondaryButton}
                      onClick={() =>
                        void handleReceiptDownload(payment.paymentId)
                      }
                      disabled={
                        actingPaymentId === payment.paymentId ||
                        !payment.receiptAvailable
                      }
                    >
                      영수증 출력
                    </button>
                    <button
                      type="button"
                      className={shellStyles.primaryButton}
                      onClick={() =>
                        void handlePaymentCancel(payment.paymentId)
                      }
                      disabled={
                        actingPaymentId === payment.paymentId ||
                        !payment.canCancel
                      }
                    >
                      결제 취소
                    </button>
                  </div>
                </article>
              ))}
            </div>
          )}
        </div>
      </section>
    </AdminMarketingShell>
  );
}

export default MyPaymentsPage;
