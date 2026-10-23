package com.sleekydz86.idolglow.productpackage.admin.application

import com.ninjasquad.springmockk.MockkBean
import com.sleekydz86.idolglow.payment.application.ReservationPaymentService
import com.sleekydz86.idolglow.payment.domain.PaymentRefundStatus
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.support.PostgresReservationFixture
import io.mockk.every
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

@DisplayName("관리자 예약 취소 감사 로그 PostgreSQL")
class AdminReservationCancelAuditPostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var fixture: PostgresReservationFixture

    @Autowired
    private lateinit var reservationPaymentService: ReservationPaymentService

    @Autowired
    private lateinit var adminReservationService: AdminReservationService

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @MockkBean
    private lateinit var tossPaymentsApiClient: TossPaymentsApiClient

    @AfterEach
    fun clearSecurity() {
        SecurityContextHolder.clearContext()
    }

    @DisplayName("환불 요청이 실패해도 관리자 취소 감사 로그를 남긴다")
    @Test
    fun adminCancelWritesAuditLogEvenIfRefundStepFails() {
        // given
        val user = fixture.persistUser()
        val created = fixture.createReservation(user.id, fixture.persistOpenSlot())
        jdbcTemplate.update(
            "update payments set payment_key = ? where id = ?",
            "pk-audit-${created.payment.paymentId}",
            created.payment.paymentId,
        )
        reservationPaymentService.handlePaymentSucceeded(created.payment.paymentReference)
        every { tossPaymentsApiClient.cancel(any(), any(), any(), any()) } throws IllegalStateException("환불 호출 실패")
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                user.id.toString(),
                "",
                listOf(SimpleGrantedAuthority("ROLE_ADMIN")),
            )

        // when
        val summary = adminReservationService.cancelReservation(created.id)

        // then
        assertEquals(created.id, summary.reservationId)
        assertEquals(ReservationStatus.CANCELED, summary.status)
        assertEquals(PaymentStatus.REFUNDING.name, paymentStatus(created.payment.paymentId))
        assertEquals(PaymentRefundStatus.PENDING.name, refundStatus(created.payment.paymentId))
        assertEquals(1, auditCount(created.id))
        assertEquals(1, refundFailureLogCount(created.payment.paymentId))
    }

    private fun paymentStatus(paymentId: Long): String =
        jdbcTemplate.queryForObject(
            "select status from payments where id = ?",
            String::class.java,
            paymentId,
        )!!

    private fun refundStatus(paymentId: Long): String =
        jdbcTemplate.queryForObject(
            "select status from payment_refunds where payment_id = ?",
            String::class.java,
            paymentId,
        )!!

    private fun auditCount(reservationId: Long): Int =
        jdbcTemplate
            .queryForObject(
                """
                select count(*) from admin_audit_logs
                where action_code = 'RESERVATION_CANCEL' and target_type = 'RESERVATION' and target_id = ?
                """.trimIndent(),
                Long::class.java,
                reservationId,
            )!!
            .toInt()

    private fun refundFailureLogCount(paymentId: Long): Int =
        jdbcTemplate
            .queryForObject(
                """
                select count(*) from payment_logs
                where payment_id = ? and log_type = 'CANCEL_ERROR' and error_message like '환불 요청에 실패했습니다.%'
                """.trimIndent(),
                Long::class.java,
                paymentId,
            )!!
            .toInt()
}
