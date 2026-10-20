package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.domain.PaymentLogStep
import com.sleekydz86.idolglow.payment.domain.PaymentLogType
import com.sleekydz86.idolglow.payment.infrastructure.PaymentJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.PaymentLogJpaRepository
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.support.PostgresReservationFixture
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@DisplayName("결제 로그 롤백 PostgreSQL")
class PaymentLogRollbackPostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var fixture: PostgresReservationFixture

    @Autowired
    private lateinit var paymentLogCommandService: PaymentLogCommandService

    @Autowired
    private lateinit var paymentLogJpaRepository: PaymentLogJpaRepository

    @Autowired
    private lateinit var paymentJpaRepository: PaymentJpaRepository

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @DisplayName("바깥 처리가 예외로 롤백돼도 결제 로그 행은 남는다")
    @Test
    fun keepsLogWhenOuterTransactionRollsBack() {
        val created = fixture.createReservation(fixture.persistUser().id, fixture.persistOpenSlot())
        val required = TransactionTemplate(transactionManager)
        runCatching {
            required.execute {
                val payment = paymentJpaRepository.findByOrderIdForUpdate(created.payment.orderId)!!
                paymentLogCommandService.append(
                    payment = payment,
                    orderId = payment.orderId,
                    paymentKey = "pk-log",
                    logType = PaymentLogType.CONFIRM_REQUEST,
                    step = PaymentLogStep.SERVER,
                    requestUrl = "/v1/payments/confirm",
                    httpMethod = "POST",
                    requestBody = """{"orderId":"${payment.orderId}"}""",
                )
                throw IllegalStateException("바깥 처리 실패")
            }
        }
        val logs = paymentLogJpaRepository.findAllByPaymentIdOrderByCreatedAtDesc(created.payment.paymentId)
        assertTrue(logs.isNotEmpty())
    }
}
