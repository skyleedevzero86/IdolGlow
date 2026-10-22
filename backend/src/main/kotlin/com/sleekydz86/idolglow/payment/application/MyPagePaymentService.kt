package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.application.dto.MyPagePaymentSummaryResponse
import com.sleekydz86.idolglow.payment.infrastructure.AdminPaymentQueryRepository
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationCancelFacade
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class MyPagePaymentService(
    private val adminPaymentQueryRepository: AdminPaymentQueryRepository,
    private val paymentViewPolicyService: PaymentViewPolicyService,
    private val paymentDocumentService: PaymentDocumentService,
    private val reservationCancelFacade: ReservationCancelFacade,
    @Lazy private val self: MyPagePaymentService,
) {
    fun findPayments(
        userId: Long,
        size: Int = 100,
    ): List<MyPagePaymentSummaryResponse> =
        adminPaymentQueryRepository
            .findPaymentsByUser(userId, size.coerceIn(1, 200))
            .map { payment ->
                MyPagePaymentSummaryResponse.from(
                    payment = payment,
                    canCancel = paymentViewPolicyService.canUserCancel(payment),
                    cancelDeadlineAt = paymentViewPolicyService.userCancelDeadline(payment),
                    receiptAvailable = paymentViewPolicyService.receiptAvailable(payment),
                )
            }

    fun findPayment(
        userId: Long,
        paymentId: Long,
    ): MyPagePaymentSummaryResponse {
        val payment =
            adminPaymentQueryRepository.findPaymentByIdAndUserId(paymentId, userId)
                ?: throw IllegalArgumentException("결제를 찾을 수 없습니다: $paymentId")
        return MyPagePaymentSummaryResponse.from(
            payment = payment,
            canCancel = paymentViewPolicyService.canUserCancel(payment),
            cancelDeadlineAt = paymentViewPolicyService.userCancelDeadline(payment),
            receiptAvailable = paymentViewPolicyService.receiptAvailable(payment),
        )
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun cancelPayment(
        userId: Long,
        paymentId: Long,
        reason: String? = null,
    ): MyPagePaymentSummaryResponse {
        reservationCancelFacade.cancelMyPagePayment(userId, paymentId, reason)
        return self.findPayment(userId, paymentId)
    }

    fun renderReceiptPdf(
        userId: Long,
        paymentId: Long,
    ): ByteArray {
        val payment =
            adminPaymentQueryRepository.findPaymentByIdAndUserId(paymentId, userId)
                ?: throw IllegalArgumentException("결제를 찾을 수 없습니다: $paymentId")
        require(paymentViewPolicyService.receiptAvailable(payment)) { "영수증을 출력할 수 없는 결제 상태입니다." }
        return paymentDocumentService.buildReceiptPdf(payment, "사용자")
    }
}
