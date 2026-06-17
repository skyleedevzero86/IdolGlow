package com.sleekydz86.idolglow.productpackage.admin.application.dto

import java.math.BigDecimal
import java.math.RoundingMode

fun computeRate(numerator: Long, denominator: Long): BigDecimal? {
    if (denominator <= 0L) return null
    return BigDecimal.valueOf(numerator)
        .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP)
}
