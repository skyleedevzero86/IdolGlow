package com.sleekydz86.idolglow.payment.application

import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.payment.infrastructure.PaymentJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.support.PostgresReservationFixture
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataAccessResourceFailureException
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger

@DisplayName("토스 승인 뒤 결과 저장 실패 PostgreSQL")
class TossConfirmSaveFailurePostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var fixture: PostgresReservationFixture

    @Autowired private lateinit var tossPaymentConfirmService: TossPaymentConfirmService

    @Autowired private lateinit var tossConfirmReconcileScheduler: TossConfirmReconcileScheduler

    @Autowired private lateinit var paymentJpaRepository: PaymentJpaRepository

    @MockkBean private lateinit var tossPaymentsApiClient: TossPaymentsApiClient

    @SpykBean private lateinit var tossPaymentConfirmTxService: TossPaymentConfirmTxService

    private val objectMapper = ObjectMapper()

    @DisplayName("토스는 승인했는데 결과 저장이 실패하면 처리 중으로 남고, 조회 스케줄러가 토스 결과로 성공 처리하며 승인은 다시 보내지 않는다")
    @Test
    fun reconcileRecoversWhenSaveFailsAfterTossApproved() {
        // given
        val user = fixture.persistUser()
        val created = fixture.createReservation(user.id, fixture.persistOpenSlot())
        val orderId = created.payment.orderId
        val approved = tossDone("pk-save-fail", orderId, created.payment.amount)
        every { tossPaymentsApiClient.confirm(any(), any(), any(), any()) } returns approved
        failFirstSave()

        // when
        val confirmed = runCatching { confirm(user.id, "pk-save-fail", orderId, created.payment.amount) }

        // then
        assertTrue(confirmed.exceptionOrNull() is DataAccessResourceFailureException)
        assertEquals(PaymentStatus.CONFIRMING, paymentJpaRepository.findByOrderId(orderId)!!.status)

        // when
        every { tossPaymentsApiClient.getPaymentByOrderId(orderId) } returns approved
        tossConfirmReconcileScheduler.reconcileConfirmingPayments(LocalDateTime.now().plusMinutes(1))

        // then
        assertEquals(PaymentStatus.SUCCEEDED, paymentJpaRepository.findByOrderId(orderId)!!.status)
        verify(exactly = 1) { tossPaymentsApiClient.confirm(any(), any(), any(), any()) }
    }

    @DisplayName("결과 저장이 실패한 뒤 사용자가 같은 승인을 다시 요청하면 처음과 같은 멱등 키로 토스에 보내 한 번만 성공 처리된다")
    @Test
    fun clientRetryAfterSaveFailureReusesIdempotencyKey() {
        // given
        val user = fixture.persistUser()
        val created = fixture.createReservation(user.id, fixture.persistOpenSlot())
        val orderId = created.payment.orderId
        val sentKeys = mutableListOf<String?>()
        every { tossPaymentsApiClient.confirm(any(), any(), any(), captureNullable(sentKeys)) } returns
            tossDone("pk-retry", orderId, created.payment.amount)
        failFirstSave()

        // when
        val first = runCatching { confirm(user.id, "pk-retry", orderId, created.payment.amount) }
        val second = confirm(user.id, "pk-retry", orderId, created.payment.amount)

        // then
        assertTrue(first.isFailure)
        assertEquals(PaymentStatus.SUCCEEDED, second.status)
        assertEquals(PaymentStatus.SUCCEEDED, paymentJpaRepository.findByOrderId(orderId)!!.status)
        assertEquals(listOf("confirm-$orderId", "confirm-$orderId"), sentKeys)
    }

    private fun failFirstSave() {
        val calls = AtomicInteger()
        every { tossPaymentConfirmTxService.completeConfirm(any(), any(), any(), any()) } answers {
            if (calls.getAndIncrement() == 0) {
                throw DataAccessResourceFailureException("결과를 저장하는 중 DB 연결이 끊겼습니다.")
            }
            callOriginal()
        }
    }

    private fun confirm(
        userId: Long,
        paymentKey: String,
        orderId: String,
        amount: BigDecimal,
    ) = tossPaymentConfirmService.confirm(userId, paymentKey, orderId, amount, null, null)

    private fun tossDone(
        paymentKey: String,
        orderId: String,
        amount: BigDecimal,
    ): TossApiResponse {
        val body =
            """{"paymentKey":"$paymentKey","orderId":"$orderId","status":"DONE",""" +
                """"totalAmount":${amount.toPlainString()},"approvedAt":"2026-11-23T10:00:00+09:00"}"""
        return TossApiResponse(200, objectMapper.readTree(body), body)
    }
}
