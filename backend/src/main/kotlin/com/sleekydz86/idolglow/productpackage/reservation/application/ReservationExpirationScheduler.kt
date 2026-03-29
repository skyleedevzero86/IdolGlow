package com.sleekydz86.idolglow.productpackage.reservation.application

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ReservationExpirationScheduler(
    private val reservationCommandService: ReservationCommandService,
) {

    @Scheduled(fixedDelayString = "\${reservation.expiration-interval-ms:30000}")
    fun expirePendingReservations() {
        reservationCommandService.expirePendingReservations()
    }

    @Scheduled(fixedDelayString = "\${reservation.expiration-interval-ms:30000}")
    fun notifyExpiringSoon() {
        reservationCommandService.notifyExpiringSoonReservations()
    }
}
