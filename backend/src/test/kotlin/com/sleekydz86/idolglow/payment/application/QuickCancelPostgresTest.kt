package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.billing.QuickBookExceptionType
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationCommandService
import com.sleekydz86.idolglow.productpackage.reservation.application.dto.CreateReservationCommand
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationBookingChannel
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.support.PostgresReservationFixture
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("결제 직후 바로 취소 PostgreSQL")
class QuickCancelPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var reservationCommandService: ReservationCommandService

    @Autowired private lateinit var fixture: PostgresReservationFixture

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    private val approvedAt = LocalDateTime.of(2026, 11, 29, 14, 0, 0)

    @DisplayName("Q2·Q3 바로 예약은 승인 30분 뒤 정각까지 취소되고, 방문 하루 전이라도 전액 환불 상태가 된다")
    @Test
    fun quickBookCancelAtThirtyMinutes() {
        // given
        val booked = book(ReservationBookingChannel.QUICK_BOOK)

        // when
        reservationCommandService.quickCancelByUser(booked.reservationId, booked.userId, approvedAt.plusMinutes(30))

        // then
        assertThat(reservationStatus(booked.reservationId)).isEqualTo("CANCELED")
        assertThat(paymentStatus(booked.reservationId)).isIn("REFUNDED", "REFUNDING")
    }

    @DisplayName("Q4 바로 예약은 승인 30분 1초 뒤 409 QUICK_CANCEL_EXPIRED 이고 예약은 그대로다")
    @Test
    fun quickBookExpiredAfterThirtyMinutes() {
        // given
        val booked = book(ReservationBookingChannel.QUICK_BOOK)

        // when
        val error =
            catchThrowableOfType(CustomException::class.java) {
                reservationCommandService.quickCancelByUser(
                    booked.reservationId,
                    booked.userId,
                    approvedAt.plusMinutes(30).plusSeconds(1),
                )
            }

        // then
        assertThat(error.getExceptionType()).isEqualTo(QuickBookExceptionType.QUICK_CANCEL_EXPIRED)
        assertThat(error.getExceptionType().httpStatusCode).isEqualTo(409)
        assertThat(reservationStatus(booked.reservationId)).isEqualTo("BOOKED")
        assertThat(paymentStatus(booked.reservationId)).isEqualTo("SUCCEEDED")
    }

    @DisplayName("Q2 자동 담기 예약은 승인 2시간 뒤 정각까지 취소되고 2시간 1초 뒤는 거절된다")
    @Test
    fun autoPickWindowIsTwoHours() {
        // given
        val inWindow = book(ReservationBookingChannel.AUTO_PICK)
        val late = book(ReservationBookingChannel.AUTO_PICK)

        // when
        reservationCommandService.quickCancelByUser(inWindow.reservationId, inWindow.userId, approvedAt.plusHours(2))
        val error =
            catchThrowableOfType(CustomException::class.java) {
                reservationCommandService.quickCancelByUser(
                    late.reservationId,
                    late.userId,
                    approvedAt.plusHours(2).plusSeconds(1),
                )
            }

        // then
        assertThat(reservationStatus(inWindow.reservationId)).isEqualTo("CANCELED")
        assertThat(error.getExceptionType()).isEqualTo(QuickBookExceptionType.QUICK_CANCEL_EXPIRED)
        assertThat(reservationStatus(late.reservationId)).isEqualTo("BOOKED")
    }

    @DisplayName("Q4 일반 결제창 예약은 승인 직후라도 바로 취소할 수 없다")
    @Test
    fun normalChannelRejected() {
        // given
        val booked = book(ReservationBookingChannel.NORMAL)

        // when
        val error =
            catchThrowableOfType(CustomException::class.java) {
                reservationCommandService.quickCancelByUser(
                    booked.reservationId,
                    booked.userId,
                    approvedAt.plusMinutes(1),
                )
            }

        // then
        assertThat(error.getExceptionType()).isEqualTo(QuickBookExceptionType.QUICK_CANCEL_EXPIRED)
        assertThat(reservationStatus(booked.reservationId)).isEqualTo("BOOKED")
    }

    @DisplayName("Q1 다른 회원의 예약은 바로 취소할 수 없다")
    @Test
    fun otherUserRejected() {
        // given
        val booked = book(ReservationBookingChannel.QUICK_BOOK)
        val other = fixture.persistUser()

        // when
        val error =
            catchThrowableOfType(IllegalArgumentException::class.java) {
                reservationCommandService.quickCancelByUser(booked.reservationId, other.id, approvedAt.plusMinutes(1))
            }

        // then
        assertThat(error.message).isEqualTo("본인 예약만 처리할 수 있습니다.")
        assertThat(reservationStatus(booked.reservationId)).isEqualTo("BOOKED")
    }

    private fun book(channel: ReservationBookingChannel): Booked {
        val user = fixture.persistUser()
        val slot = fixture.persistOpenSlot()
        val created =
            reservationCommandService.createReservation(
                CreateReservationCommand(slot.productId, slot.slotId, user.id, slot.totalPrice, channel),
            )
        jdbcTemplate.update(
            "update reservations set status = 'BOOKED', confirmed_at = ?, visit_date = ? where id = ?",
            approvedAt,
            LocalDate.now().plusDays(1),
            created.id,
        )
        jdbcTemplate.update(
            "update payments set status = 'SUCCEEDED', approved_at = ?, " +
                "payment_key = concat('pk-quick-', id) where reservation_id = ?",
            approvedAt,
            created.id,
        )
        return Booked(created.id, user.id)
    }

    private fun reservationStatus(id: Long): String =
        jdbcTemplate.queryForObject("select status from reservations where id = ?", String::class.java, id)!!

    private fun paymentStatus(reservationId: Long): String =
        jdbcTemplate.queryForObject(
            "select status from payments where reservation_id = ?",
            String::class.java,
            reservationId,
        )!!

    private data class Booked(
        val reservationId: Long,
        val userId: Long,
    )
}
