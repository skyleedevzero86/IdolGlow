package com.sleekydz86.idolglow.survey.application.autopick

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.billing.QuickBookExceptionType
import com.sleekydz86.idolglow.payment.application.QuickBookService
import com.sleekydz86.idolglow.productpackage.reservation.application.dto.CreateReservationCommand
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationBookingChannel
import com.sleekydz86.idolglow.survey.domain.autopick.SurveyAutoPickFailReason
import com.sleekydz86.idolglow.survey.domain.autopick.SurveyAutoPickStatus
import com.sleekydz86.idolglow.survey.infrastructure.autopick.SurveyAutoPickCandidateJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class SurveyAutoPickChargeService(
    private val candidateRepository: SurveyAutoPickCandidateJpaRepository,
    private val txService: SurveyAutoPickTxService,
    private val quickBookService: QuickBookService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun chargeDue(now: LocalDateTime = LocalDateTime.now()): Int =
        candidateRepository
            .findDueIds(SurveyAutoPickStatus.SCHEDULED, now)
            .count { candidateId -> txService.claim(candidateId, now)?.let { charge(it) } == true }

    private fun charge(claim: AutoPickClaim): Boolean {
        val command =
            CreateReservationCommand(
                productId = claim.productId,
                reservationSlotId = claim.reservationSlotId,
                userId = claim.userId,
                totalPrice = claim.totalPrice,
                bookingChannel = ReservationBookingChannel.AUTO_PICK,
            )
        return try {
            val payment = quickBookService.book(command)
            txService.book(claim.candidateId, payment.reservationId)
            true
        } catch (error: CustomException) {
            fail(claim, reasonOf(error), error)
        } catch (error: IllegalArgumentException) {
            fail(claim, SurveyAutoPickFailReason.SLOT_UNAVAILABLE, error)
        } catch (error: IllegalStateException) {
            fail(claim, SurveyAutoPickFailReason.SLOT_UNAVAILABLE, error)
        }
    }

    private fun fail(
        claim: AutoPickClaim,
        reason: SurveyAutoPickFailReason,
        error: RuntimeException,
    ): Boolean {
        log.warn("자동 담기 예약 실패 candidateId={} userId={} reason={}", claim.candidateId, claim.userId, reason, error)
        txService.fail(claim.candidateId, reason)
        return false
    }

    private fun reasonOf(error: CustomException): SurveyAutoPickFailReason =
        when (error.getExceptionType()) {
            QuickBookExceptionType.BILLING_METHOD_REQUIRED -> SurveyAutoPickFailReason.NO_BILLING_METHOD
            QuickBookExceptionType.PAYMENT_FAILED -> SurveyAutoPickFailReason.PAYMENT_FAILED
            else -> SurveyAutoPickFailReason.SLOT_UNAVAILABLE
        }
}
