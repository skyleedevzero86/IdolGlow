package com.sleekydz86.idolglow.global.infrastructure.exception.reservation

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomExceptionType

enum class ReservationExceptionType(
    override val errorCode: String,
    override val message: String,
    override val httpStatusCode: Int,
) : CustomExceptionType {
    SLOT_ALREADY_BOOKED("RESERVATION_SLOT_ALREADY_BOOKED", "이미 예약된 슬롯입니다.", CONFLICT),
    SLOT_HELD("RESERVATION_SLOT_HELD", "다른 예약이 이미 선점한 슬롯입니다.", CONFLICT),
}

private const val CONFLICT = 409
