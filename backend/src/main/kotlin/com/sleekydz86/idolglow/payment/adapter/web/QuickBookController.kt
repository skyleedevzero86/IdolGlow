package com.sleekydz86.idolglow.payment.adapter.web

import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import com.sleekydz86.idolglow.payment.adapter.web.request.QuickBookRequest
import com.sleekydz86.idolglow.payment.application.QuickBookService
import com.sleekydz86.idolglow.payment.application.dto.PaymentResponse
import com.sleekydz86.idolglow.payment.application.dto.QuickCancelResponse
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationCancelFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "바로 예약", description = "기본 카드로 바로 예약하고 결제 직후 바로 취소")
@RestController
@RequestMapping("/reservations")
class QuickBookController(
    private val quickBookService: QuickBookService,
    private val reservationCancelFacade: ReservationCancelFacade,
) {
    @Operation(summary = "기본 카드로 바로 예약")
    @PostMapping("/quick-book")
    fun quickBook(
        @LoginUser userId: Long,
        @Valid @RequestBody request: QuickBookRequest,
    ): ResponseEntity<PaymentResponse> = ResponseEntity.ok(quickBookService.book(request.toCommand(userId)))

    @Operation(summary = "결제 직후 바로 취소")
    @PostMapping("/{reservationId}/quick-cancel")
    fun quickCancel(
        @LoginUser userId: Long,
        @PathVariable reservationId: Long,
    ): ResponseEntity<QuickCancelResponse> =
        ResponseEntity.ok(QuickCancelResponse(reservationCancelFacade.quickCancelByUser(reservationId, userId)))
}
