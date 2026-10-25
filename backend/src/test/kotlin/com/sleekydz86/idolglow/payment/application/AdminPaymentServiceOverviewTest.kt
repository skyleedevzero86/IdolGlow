package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.payment.infrastructure.AdminPaymentQueryRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

@DisplayName("관리자 결제 개요")
class AdminPaymentServiceOverviewTest {
    private val repository = mockk<AdminPaymentQueryRepository>()
    private val service =
        AdminPaymentService(
            adminPaymentQueryRepository = repository,
            paymentRefundJpaRepository = mockk(relaxed = true),
            paymentLogJpaRepository = mockk(relaxed = true),
            reservationCancelFacade = mockk(relaxed = true),
            paymentViewPolicyService = mockk(relaxed = true),
            paymentDocumentService = mockk(relaxed = true),
            self = mockk(relaxed = true),
        )

    @Test
    fun overviewCountsProcessingAndTenMinuteStale() {
        val items =
            listOf(
                payment(PaymentStatus.CONFIRMING, minutesAgo = 11),
                payment(PaymentStatus.CONFIRMING, minutesAgo = 1),
                payment(PaymentStatus.REFUNDING, minutesAgo = 40),
                payment(PaymentStatus.SUCCEEDED, minutesAgo = 11),
            )
        every { repository.findPayments(null, null, null, 5_000) } returns items

        val overview = service.overview(status = null, visitDate = null, productId = null)

        assertEquals(3L, overview.processingCount)
        assertEquals(2L, overview.processingStaleCount)
    }

    private fun payment(
        status: PaymentStatus,
        minutesAgo: Long,
    ): Payment {
        val reservation = mockk<com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation>(relaxed = true)
        every { reservation.totalPrice } returns BigDecimal("10000")
        val created = Payment.createPendingForToss(reservation, "pay-${status.name}-$minutesAgo")
        created.markConfirming()
        if (status == PaymentStatus.SUCCEEDED || status == PaymentStatus.REFUNDING) {
            created.markSucceeded()
        }
        if (status == PaymentStatus.REFUNDING) {
            created.markRefunding()
        }
        val field = BaseEntity::class.java.getDeclaredField("updatedAt")
        field.isAccessible = true
        field.set(created, LocalDateTime.now().minusMinutes(minutesAgo))
        return created
    }
}
