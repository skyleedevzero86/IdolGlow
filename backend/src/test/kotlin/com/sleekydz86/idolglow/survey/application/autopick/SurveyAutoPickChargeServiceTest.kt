package com.sleekydz86.idolglow.survey.application.autopick

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.billing.QuickBookExceptionType
import com.sleekydz86.idolglow.payment.application.QuickBookService
import com.sleekydz86.idolglow.payment.application.dto.PaymentResponse
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationBookingChannel
import com.sleekydz86.idolglow.survey.domain.autopick.SurveyAutoPickFailReason
import com.sleekydz86.idolglow.survey.domain.autopick.SurveyAutoPickStatus
import com.sleekydz86.idolglow.survey.infrastructure.autopick.SurveyAutoPickCandidateJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

@DisplayName("자동 담기 결제")
class SurveyAutoPickChargeServiceTest {
    private val candidateRepository = mockk<SurveyAutoPickCandidateJpaRepository>()
    private val txService = mockk<SurveyAutoPickTxService>(relaxed = true)
    private val quickBookService = mockk<QuickBookService>()
    private val service = SurveyAutoPickChargeService(candidateRepository, txService, quickBookService)
    private val now = LocalDateTime.of(2026, 11, 29, 12, 0)
    private val claim = AutoPickClaim(30L, 7L, 501L, 9001L, BigDecimal("44100.00"))

    @BeforeEach
    fun setUp() {
        every { candidateRepository.findDueIds(SurveyAutoPickStatus.SCHEDULED, now) } returns listOf(30L)
        every { txService.claim(30L, now) } returns claim
    }

    @DisplayName("A7 자동 예약 경로로 바로 예약하고 성공하면 예약 번호를 후보에 남긴다")
    @Test
    fun booksWithAutoPickChannel() {
        // given
        val payment = mockk<PaymentResponse>()
        every { payment.reservationId } returns 77L
        every { quickBookService.book(any()) } returns payment

        // when
        val booked = service.chargeDue(now)

        // then
        assertThat(booked).isEqualTo(1)
        verify {
            quickBookService.book(
                match {
                    it.bookingChannel == ReservationBookingChannel.AUTO_PICK &&
                        it.reservationSlotId == 9001L &&
                        it.totalPrice.compareTo(BigDecimal("44100")) == 0
                },
            )
        }
        verify { txService.book(30L, 77L) }
    }

    @DisplayName("A8 카드 결제가 실패하면 PAYMENT_FAILED 로 남긴다")
    @Test
    fun paymentFailure() {
        // given
        every { quickBookService.book(any()) } throws CustomException(QuickBookExceptionType.PAYMENT_FAILED)

        // when
        val booked = service.chargeDue(now)

        // then
        assertThat(booked).isZero()
        verify { txService.fail(30L, SurveyAutoPickFailReason.PAYMENT_FAILED) }
        verify(exactly = 0) { txService.book(any(), any()) }
    }

    @DisplayName("A8 슬롯이 마감돼 예약을 만들지 못하면 SLOT_UNAVAILABLE 로 남긴다")
    @Test
    fun slotUnavailable() {
        // given
        every { quickBookService.book(any()) } throws IllegalArgumentException("이미 예약된 슬롯입니다.")

        // when
        service.chargeDue(now)

        // then
        verify { txService.fail(30L, SurveyAutoPickFailReason.SLOT_UNAVAILABLE) }
    }

    @DisplayName("D14 집지 못한 후보는 결제하지 않는다")
    @Test
    fun unclaimedNotCharged() {
        // given
        every { txService.claim(30L, now) } returns null

        // when
        val booked = service.chargeDue(now)

        // then
        assertThat(booked).isZero()
        verify(exactly = 0) { quickBookService.book(any()) }
    }
}
