package com.sleekydz86.idolglow.payment.graphql

import com.sleekydz86.idolglow.graphql.PaymentGraphQlResponse
import com.sleekydz86.idolglow.payment.application.ReservationPaymentService
import com.sleekydz86.idolglow.payment.ui.request.MockPaymentWebhookRequest
import com.sleekydz86.idolglow.payment.ui.request.MockPaymentWebhookStatus
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.stereotype.Controller

@Controller
class PaymentGraphQlController(
    private val reservationPaymentService: ReservationPaymentService,
    @Value("\${payment.mock.webhook-secret}")
    private val webhookSecret: String,
) {

    @MutationMapping
    fun mockPaymentWebhook(
        @Argument secret: String,
        @Argument @Valid input: MockPaymentWebhookRequest,
    ): PaymentGraphQlResponse {
        require(secret == webhookSecret) { "웹훅 시크릿이 올바르지 않습니다." }

        val payment = when (input.status) {
            MockPaymentWebhookStatus.SUCCEEDED ->
                reservationPaymentService.handlePaymentSucceeded(input.paymentReference)

            MockPaymentWebhookStatus.FAILED ->
                reservationPaymentService.handlePaymentFailed(
                    paymentReference = input.paymentReference,
                    reason = input.failureReason ?: "모의 결제가 실패했습니다."
                )

            MockPaymentWebhookStatus.CANCELED ->
                reservationPaymentService.handlePaymentCanceled(
                    paymentReference = input.paymentReference,
                    reason = input.failureReason ?: "모의 결제가 취소되었습니다."
                )
        }

        return PaymentGraphQlResponse.from(payment)
    }
}
