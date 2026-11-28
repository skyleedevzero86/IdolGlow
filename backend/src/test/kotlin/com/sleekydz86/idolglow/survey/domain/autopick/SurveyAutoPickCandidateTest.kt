package com.sleekydz86.idolglow.survey.domain.autopick

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("자동 담기 후보 상태 변화")
class SurveyAutoPickCandidateTest {
    private val chargeAt = LocalDateTime.of(2026, 12, 1, 10, 10)

    @DisplayName("D13 결제 예정 1초 전에는 건너뛸 수 있고 결제 예정 시각에는 건너뛸 수 없다")
    @Test
    fun skipBeforeChargeOnly() {
        // given
        val early = candidate()
        val onTime = candidate()

        // when
        early.skip(chargeAt.minusSeconds(1))

        // then
        assertThat(early.status).isEqualTo(SurveyAutoPickStatus.SKIPPED)
        assertThat(onTime.canSkip(chargeAt)).isFalse()
        assertThatThrownBy { onTime.skip(chargeAt) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("결제 예정 시각 전의 예정 후보만 건너뛸 수 있습니다.")
        assertThat(onTime.status).isEqualTo(SurveyAutoPickStatus.SCHEDULED)
    }

    @DisplayName("D12 건너뛴 후보는 결제를 시작할 수 없다")
    @Test
    fun skippedCannotProcess() {
        // given
        val candidate = candidate()
        candidate.skip(chargeAt.minusMinutes(1))

        // when
        val error = runCatching { candidate.startProcessing() }.exceptionOrNull()

        // then
        assertThat(error).isInstanceOf(IllegalStateException::class.java)
        assertThat(candidate.status).isEqualTo(SurveyAutoPickStatus.SKIPPED)
    }

    @DisplayName("D12 예정 후보는 바로 예약 완료가 될 수 없고 결제 중을 거쳐야 한다")
    @Test
    fun bookRequiresProcessing() {
        // given
        val candidate = candidate()

        // when
        val error = runCatching { candidate.book(10L) }.exceptionOrNull()
        candidate.startProcessing()
        candidate.book(10L)

        // then
        assertThat(error).isInstanceOf(IllegalStateException::class.java)
        assertThat(candidate.status).isEqualTo(SurveyAutoPickStatus.BOOKED)
        assertThat(candidate.reservationId).isEqualTo(10L)
    }

    @DisplayName("D12 예약 완료된 후보는 실패로 바뀌지 않는다")
    @Test
    fun bookedCannotFail() {
        // given
        val candidate = candidate()
        candidate.startProcessing()
        candidate.book(10L)

        // when
        val error = runCatching { candidate.fail(SurveyAutoPickFailReason.PAYMENT_FAILED) }.exceptionOrNull()

        // then
        assertThat(error).isInstanceOf(IllegalStateException::class.java)
        assertThat(candidate.status).isEqualTo(SurveyAutoPickStatus.BOOKED)
        assertThat(candidate.failReason).isNull()
    }

    private fun candidate() =
        SurveyAutoPickCandidate(
            userId = 1L,
            productId = 2L,
            reservationSlotId = 3L,
            memberOpenAt = chargeAt.minusMinutes(10),
            scheduledChargeAt = chargeAt,
        )
}
