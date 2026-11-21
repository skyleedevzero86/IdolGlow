package com.sleekydz86.idolglow.eventinfo.domain

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object FestivalCoordinateMatch {
    const val MAX_DISTANCE_METERS: Double = 200.0
    private const val EARTH_RADIUS_METERS: Double = 6_371_000.0

    fun findSingle(
        target: FestivalEvent,
        candidates: List<FestivalEvent>,
    ): FestivalEvent? {
        if (!hasPeriodAndPoint(target)) return null
        return candidates.filter { isSameFestival(target, it) }.singleOrNull()
    }

    private fun isSameFestival(
        target: FestivalEvent,
        candidate: FestivalEvent,
    ): Boolean =
        hasPeriodAndPoint(candidate) &&
            target.eventStartDate == candidate.eventStartDate &&
            target.eventEndDate == candidate.eventEndDate &&
            distanceMeters(target, candidate) <= MAX_DISTANCE_METERS

    private fun hasPeriodAndPoint(event: FestivalEvent): Boolean =
        !event.eventStartDate.isNullOrBlank() &&
            !event.eventEndDate.isNullOrBlank() &&
            (event.mapX ?: 0.0) != 0.0 &&
            (event.mapY ?: 0.0) != 0.0

    private fun distanceMeters(
        a: FestivalEvent,
        b: FestivalEvent,
    ): Double {
        val lat1 = Math.toRadians(requireNotNull(a.mapY))
        val lat2 = Math.toRadians(requireNotNull(b.mapY))
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(requireNotNull(b.mapX) - requireNotNull(a.mapX))
        val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(h))
    }
}
