package com.sleekydz86.idolglow.survey.infrastructure.autopick

import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationCommandService
import com.sleekydz86.idolglow.productpackage.reservation.application.dto.CreateReservationCommand
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationBookingChannel
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import com.sleekydz86.idolglow.survey.application.autopick.SurveyAutoPickMonthlyCounter
import com.sleekydz86.idolglow.survey.domain.autopick.SurveyAutoPickCandidate
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.support.PostgresReservationFixture
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("자동 담기 조회 PostgreSQL")
class SurveyAutoPickRepositoryPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var candidateRepository: SurveyAutoPickCandidateJpaRepository

    @Autowired private lateinit var counter: SurveyAutoPickMonthlyCounter

    @Autowired private lateinit var fixture: PostgresReservationFixture

    @Autowired private lateinit var reservationCommandService: ReservationCommandService

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    @DisplayName("D17 대상 상품의 날짜 범위 안 슬롯만 찾는다")
    @Test
    fun findsSlotsOfTargetProductsInRange() {
        // given
        val target = fixture.persistOpenSlot()
        val other = fixture.persistOpenSlot()
        val visitDate = LocalDate.now().plusDays(3)

        // when
        val inRange = candidateRepository.findSlotsOfProducts(setOf(target.productId), visitDate, visitDate)
        val outOfRange =
            candidateRepository.findSlotsOfProducts(
                setOf(target.productId),
                visitDate.plusDays(1),
                visitDate.plusDays(2),
            )

        // then
        assertThat(inRange.map { it.id }).contains(target.slotId)
        assertThat(inRange.map { it.product.id }.toSet()).containsExactly(target.productId)
        assertThat(inRange.map { it.id }).doesNotContain(other.slotId)
        assertThat(outOfRange).isEmpty()
    }

    @DisplayName("D16 이번 달 확정된 AUTO_PICK 예약만 세고 일반 예약과 다음 달 확정은 뺀다")
    @Test
    fun countsAutoPickBookedThisMonthOnly() {
        // given
        val user = fixture.persistUser()
        val now = LocalDateTime.of(2031, 5, 31, 23, 0)
        reserve(user.id, ReservationBookingChannel.AUTO_PICK, LocalDateTime.of(2031, 5, 1, 0, 0))
        reserve(user.id, ReservationBookingChannel.AUTO_PICK, LocalDateTime.of(2031, 6, 1, 0, 0))
        reserve(user.id, ReservationBookingChannel.NORMAL, LocalDateTime.of(2031, 5, 10, 0, 0))
        reserve(user.id, ReservationBookingChannel.AUTO_PICK, LocalDateTime.of(2031, 4, 30, 23, 59, 59))

        // when
        val count = counter.bookedInMonth(user.id, now)

        // then
        assertThat(count).isEqualTo(1L)
    }

    @DisplayName("A10 같은 회원과 같은 슬롯 후보를 두 번 저장하면 고유 제약으로 막는다")
    @Test
    fun uniqueUserSlot() {
        // given
        val user = fixture.persistUser()
        val slot = fixture.persistOpenSlot()
        candidateRepository.saveAndFlush(candidate(user.id, slot.productId, slot.slotId))

        // when
        val duplicate = { candidateRepository.saveAndFlush(candidate(user.id, slot.productId, slot.slotId)) }

        // then
        assertThatThrownBy { duplicate() }.isInstanceOf(DataIntegrityViolationException::class.java)
        assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from survey_auto_pick_candidates where user_id = ? and reservation_slot_id = ?",
                Long::class.java,
                user.id,
                slot.slotId,
            ),
        ).isEqualTo(1L)
    }

    private fun reserve(
        userId: Long,
        channel: ReservationBookingChannel,
        confirmedAt: LocalDateTime,
    ) {
        val slot = fixture.persistOpenSlot()
        val created =
            reservationCommandService.createReservation(
                CreateReservationCommand(slot.productId, slot.slotId, userId, slot.totalPrice, channel),
            )
        jdbcTemplate.update(
            "update reservations set status = ?, confirmed_at = ? where id = ?",
            ReservationStatus.BOOKED.name,
            confirmedAt,
            created.id,
        )
    }

    private fun candidate(
        userId: Long,
        productId: Long,
        slotId: Long,
    ) = SurveyAutoPickCandidate(
        userId = userId,
        productId = productId,
        reservationSlotId = slotId,
        memberOpenAt = LocalDateTime.of(2031, 5, 1, 10, 0),
        scheduledChargeAt = LocalDateTime.of(2031, 5, 1, 10, 10),
    )
}
