package com.sleekydz86.idolglow.productpackage.reservation.application

import com.sleekydz86.idolglow.payment.application.GlowPassBenefitPolicy
import com.sleekydz86.idolglow.productpackage.reservation.application.dto.OpenReservationSlotResponse
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationSlotRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Transactional(readOnly = true)
@Service
class ReservationSlotQueryService(
    private val reservationSlotRepository: ReservationSlotRepository,
    private val reservationCommandService: ReservationCommandService,
    private val benefitPolicy: GlowPassBenefitPolicy,
) {
    fun upcomingSlots(
        productId: Long,
        userId: Long,
        now: LocalDateTime = LocalDateTime.now(),
    ): List<OpenReservationSlotResponse> {
        val price = reservationCommandService.expectedTotalPrice(productId, userId, now)
        return reservationSlotRepository
            .findAllByProductId(productId)
            .filter { LocalDateTime.of(it.reservationDate, it.startTime).isAfter(now) }
            .take(MAX_SLOTS)
            .map { slot ->
                OpenReservationSlotResponse(
                    reservationSlotId = slot.id,
                    reservationDate = slot.reservationDate,
                    startTime = slot.startTime,
                    endTime = slot.endTime,
                    available = !slot.isUnavailableForWaitlistRegistration(now),
                    memberOpenAt = benefitPolicy.memberOpensAt(slot),
                    publicOpenAt = benefitPolicy.publicOpensAt(slot),
                    totalPrice = price,
                )
            }
    }

    private companion object {
        const val MAX_SLOTS = 30
    }
}
