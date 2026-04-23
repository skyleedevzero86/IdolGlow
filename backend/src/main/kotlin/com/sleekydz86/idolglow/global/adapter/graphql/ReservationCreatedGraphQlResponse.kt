package com.sleekydz86.idolglow.global.graphql

import com.sleekydz86.idolglow.global.graphql.asGraphQlId
import com.sleekydz86.idolglow.global.graphql.asGraphQlValue
import com.sleekydz86.idolglow.productpackage.reservation.application.dto.ReservationCreatedResponse

data class ReservationCreatedGraphQlResponse(
    val id: String,
    val status: String,
    val expiresAt: String?,
    val payment: PaymentGraphQlResponse,
) {
    companion object {
        fun from(response: ReservationCreatedResponse): ReservationCreatedGraphQlResponse =
            ReservationCreatedGraphQlResponse(
                id = response.id.asGraphQlId(),
                status = response.status.name,
                expiresAt = response.expiresAt.asGraphQlValue(),
                payment = PaymentGraphQlResponse.from(response)
            )
    }
}
