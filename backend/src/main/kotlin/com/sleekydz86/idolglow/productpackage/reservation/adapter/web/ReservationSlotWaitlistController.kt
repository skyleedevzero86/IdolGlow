package com.sleekydz86.idolglow.productpackage.reservation.ui

import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationSlotWaitlistService
import com.sleekydz86.idolglow.productpackage.reservation.application.dto.SlotWaitlistEntryResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
class ReservationSlotWaitlistController(
    private val reservationSlotWaitlistService: ReservationSlotWaitlistService,
) : ReservationSlotWaitlistApi {

    @PostMapping("/products/{productId}/reservation-slots/{reservationSlotId}/waitlist")
    @ResponseStatus(HttpStatus.CREATED)
    override fun register(
        @LoginUser userId: Long,
        @PathVariable productId: Long,
        @PathVariable reservationSlotId: Long,
    ): SlotWaitlistEntryResponse =
        reservationSlotWaitlistService.register(userId, productId, reservationSlotId)

    @DeleteMapping("/products/{productId}/reservation-slots/{reservationSlotId}/waitlist")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun unregister(
        @LoginUser userId: Long,
        @PathVariable productId: Long,
        @PathVariable reservationSlotId: Long,
    ) {
        reservationSlotWaitlistService.unregister(userId, productId, reservationSlotId)
    }

    @GetMapping("/reservation-slot-waitlist")
    override fun findMine(@LoginUser userId: Long): List<SlotWaitlistEntryResponse> =
        reservationSlotWaitlistService.findMine(userId)
}
