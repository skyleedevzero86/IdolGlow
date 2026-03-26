package com.sleekydz86.idolglow.productpackage.reservation.ui

import com.sleekydz86.idolglow.global.resolver.LoginUser
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationCommandService
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationQueryService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/products/{productId}/reservations")
class ReservationController(
    private val reservationCommandService: ReservationCommandService,
    private val reservationQueryService: ReservationQueryService,
) : ReservationApi {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun createReservation(
        @LoginUser userId: Long,
        @PathVariable productId: Long,
        @Valid @RequestBody request: CreateReservationRequest
    ): ResponseEntity<ReservationCreatedResponse> {
        val reservation = reservationCommandService.createReservation(request.toCommand(userId, productId))
        return ResponseEntity.created(URI.create("/products/$productId/reservations/${reservation.id}"))
            .body(reservation)
    }

    @PostMapping("/{reservationId}/cancel")
    override fun cancelReservation(
        @LoginUser userId: Long,
        @PathVariable productId: Long,
        @PathVariable reservationId: Long,
    ): ResponseEntity<ReservationSummaryResponse> {
        val existing = reservationQueryService.findReservationDetail(reservationId, userId)
        require(existing.productId == productId) { "Reservation does not belong to product $productId." }
        reservationCommandService.cancelReservationByUser(reservationId, userId)
        return ResponseEntity.ok(
            reservationQueryService.findReservationDetail(reservationId, userId)
        )
    }
}
