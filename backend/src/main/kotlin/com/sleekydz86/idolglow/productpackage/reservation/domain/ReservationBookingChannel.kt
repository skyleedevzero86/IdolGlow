package com.sleekydz86.idolglow.productpackage.reservation.domain

import java.time.Duration

enum class ReservationBookingChannel(
    val quickCancelWindow: Duration?,
) {
    NORMAL(quickCancelWindow = null),
    QUICK_BOOK(quickCancelWindow = Duration.ofMinutes(QUICK_BOOK_CANCEL_MINUTES)),
    AUTO_PICK(quickCancelWindow = Duration.ofHours(AUTO_PICK_CANCEL_HOURS)),
}

private const val QUICK_BOOK_CANCEL_MINUTES = 30L
private const val AUTO_PICK_CANCEL_HOURS = 2L
