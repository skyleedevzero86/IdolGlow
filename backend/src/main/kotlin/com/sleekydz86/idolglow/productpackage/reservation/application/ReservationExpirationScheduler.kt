package com.sleekydz86.idolglow.productpackage.reservation.application

import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ReservationExpirationScheduler(
    private val reservationCommandService: ReservationCommandService,
    private val reservationRepository: ReservationRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${reservation.expiration-interval-ms:30000}")
    fun expirePendingReservations() {
        val now = LocalDateTime.now()
        reservationRepository.findExpiredPendingIds(EXPIRATION_BATCH_SIZE, now).forEach { reservationId ->
            runCatching { reservationCommandService.expirePendingReservation(reservationId, now) }
                .onFailure { error -> log.warn("만료 처리 실패 reservationId={}", reservationId, error) }
        }
    }

    @Scheduled(fixedDelayString = "\${reservation.expiration-interval-ms:30000}")
    fun notifyExpiringSoon() {
        reservationCommandService.notifyExpiringSoonReservations()
    }

    companion object {
        private const val EXPIRATION_BATCH_SIZE = 100
    }
}
