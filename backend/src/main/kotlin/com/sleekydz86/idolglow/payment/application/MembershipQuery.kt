package com.sleekydz86.idolglow.payment.application

import java.time.LocalDateTime

interface MembershipQuery {
    fun isActive(
        userId: Long,
        at: LocalDateTime = LocalDateTime.now(),
    ): Boolean
}
