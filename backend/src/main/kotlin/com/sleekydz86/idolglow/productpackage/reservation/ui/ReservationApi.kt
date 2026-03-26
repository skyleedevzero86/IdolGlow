package com.sleekydz86.idolglow.productpackage.reservation.ui

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity

@Tag(name = "Reservation", description = "Reservation API")
interface ReservationApi {

    @Operation(
        summary = "Create reservation",
        description = "Create a pending reservation and return payment information."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Created",
                headers = [
                    Header(
                        name = "Location",
                        description = "Reservation URI",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid request",
                content = [Content(schema = Schema(hidden = true))]
            )
        ]
    )
    fun createReservation(
        @Parameter(hidden = true)
        userId: Long,
        @Parameter(description = "Product id", example = "1")
        productId: Long,
        @Valid request: CreateReservationRequest
    ): ResponseEntity<ReservationCreatedResponse>

    @Operation(
        summary = "Cancel reservation",
        description = "Cancel the reservation owned by the authenticated user."
    )
    fun cancelReservation(
        @Parameter(hidden = true)
        userId: Long,
        @Parameter(description = "Product id", example = "1")
        productId: Long,
        @Parameter(description = "Reservation id", example = "1")
        reservationId: Long,
    ): ResponseEntity<ReservationSummaryResponse>
}
