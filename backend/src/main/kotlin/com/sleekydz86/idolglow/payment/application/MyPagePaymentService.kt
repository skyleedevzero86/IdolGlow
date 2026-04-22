package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.application.dto.MyPagePaymentSummaryResponse
import com.sleekydz86.idolglow.payment.domain.PaymentRepository
import com.sleekydz86.idolglow.payment.infrastructure.AdminPaymentQueryRepository
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationCommandService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class MyPagePaymentService(
    private val adminPaymentQueryRepository: AdminPaymentQueryRepository,
    private val paymentRepository: PaymentRepository,
    private val reservationCommandService: ReservationCommandService,
    private val paymentViewPolicyService: PaymentViewPolicyService,
    private val paymentDocumentService: PaymentDocumentService,
) {

    fun findPayments(userId: Long, size: Int = 100): List<MyPagePaymentSummaryResponse> =
        adminPaymentQueryRepository.findPaymentsByUser(userId, size.coerceIn(1, 200))
            .map { payment ->
                MyPagePaymentSummaryResponse.from(
                    payment = payment,
                    canCancel = paymentViewPolicyService.canUserCancel(payment),
                    cancelDeadlineAt = paymentViewPolicyService.userCancelDeadline(payment),
                    receiptAvailable = paymentViewPolicyService.receiptAvailable(payment),
                )
            }

    fun findPayment(userId: Long, paymentId: Long): MyPagePaymentSummaryResponse {
        val payment = adminPaymentQueryRepository.findPaymentByIdAndUserId(paymentId, userId)
            ?: throw IllegalArgumentException("결제를 찾을 수 없습니다: $paymentId")
        return MyPagePaymentSummaryResponse.from(
            payment = payment,
            canCancel = paymentViewPolicyService.canUserCancel(payment),
            cancelDeadlineAt = paymentViewPolicyService.userCancelDeadline(payment),
            receiptAvailable = paymentViewPolicyService.receiptAvailable(payment),
        )
    }

    @Transactional
    fun cancelPayment(userId: Long, paymentId: Long, reason: String? = null): MyPagePaymentSummaryResponse {
        val payment = paymentRepository.findByIdForUpdate(paymentId)
            ?: throw IllegalArgumentException("결제를 찾을 수 없습니다: $paymentId")
        require(payment.reservation.userId == userId) { "본인 결제만 취소할 수 있습니다." }
        require(paymentViewPolicyService.canUserCancel(payment)) { "사용자 결제 취소 가능 기간(15일)이 지났거나 취소할 수 없는 상태입니다." }
        reservationCommandService.cancelReservationByUser(payment.reservation.id, userId, reason)
        return findPayment(userId, paymentId)
    }

    fun renderReceiptPdf(userId: Long, paymentId: Long): ByteArray {
        val payment = adminPaymentQueryRepository.findPaymentByIdAndUserId(paymentId, userId)
            ?: throw IllegalArgumentException("결제를 찾을 수 없습니다: $paymentId")
        require(paymentViewPolicyService.receiptAvailable(payment)) { "영수증을 출력할 수 없는 결제 상태입니다." }
        return paymentDocumentService.buildReceiptPdf(payment, "사용자")
    }
}
