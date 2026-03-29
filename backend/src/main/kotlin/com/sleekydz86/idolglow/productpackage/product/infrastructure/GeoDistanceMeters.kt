package com.sleekydz86.idolglow.productpackage.product.infrastructure

import java.math.BigDecimal
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object GeoDistanceMeters {
    private const val EARTH_RADIUS_M = 6_371_000.0

    fun between(
        lat1: BigDecimal,
        lng1: BigDecimal,
        lat2: BigDecimal,
        lng2: BigDecimal,
    ): Double {
        val φ1 = Math.toRadians(lat1.toDouble())
        val φ2 = Math.toRadians(lat2.toDouble())
        val Δφ = Math.toRadians(lat2.toDouble() - lat1.toDouble())
        val Δλ = Math.toRadians(lng2.toDouble() - lng1.toDouble())
        val a = sin(Δφ / 2).pow(2) + cos(φ1) * cos(φ2) * sin(Δλ / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_M * c
    }
}
