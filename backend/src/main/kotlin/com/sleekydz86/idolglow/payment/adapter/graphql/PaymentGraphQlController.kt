package com.sleekydz86.idolglow.payment.adapter.graphql

import com.sleekydz86.idolglow.payment.adapter.graphql.PaymentGraphQlResponse
import com.sleekydz86.idolglow.payment.adapter.web.request.MockPaymentWebhookRequest
import com.sleekydz86.idolglow.payment.adapter.web.request.MockPaymentWebhookStatus
import com.sleekydz86.idolglow.payment.application.ReservationPaymentService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Profile("local", "test")
@Controller
@PreAuthorize("hasRole('ADMIN')")
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
        require(webhookSecret.isNotBlank()) { "payment.mock.webhook-secret 값이 비어 있습니다." }
        require(secret == webhookSecret) { "웹훅 시크릿이 올바르지 않습니다." }
        reservationPaymentService.requireMockPayment(input.paymentReference)

        val payment =
            when (input.status) {
                MockPaymentWebhookStatus.SUCCEEDED ->
                    reservationPaymentService.handlePaymentSucceeded(input.paymentReference)

                MockPaymentWebhookStatus.FAILED ->
                    reservationPaymentService.handlePaymentFailed(
                        paymentReference = input.paymentReference,
                        reason = input.failureReason ?: "모의 결제가 실패했습니다.",
                    )

                MockPaymentWebhookStatus.CANCELED ->
                    reservationPaymentService.handlePaymentCanceled(
                        paymentReference = input.paymentReference,
                        reason = input.failureReason ?: "모의 결제가 취소되었습니다.",
                    )
            }

        return PaymentGraphQlResponse.from(payment)
    }
}
