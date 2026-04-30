package com.sleekydz86.idolglow.glowweather.domain

import java.util.Locale

object WindDirection {
    fun degreesFromCompassAbbreviation(abbr: String): Int? {
        val k = abbr.trim().uppercase(Locale.getDefault())
        if (k.isEmpty()) return null
        return COMPASS_ABBREV_TO_KMA_DEG[k]
    }

    private val COMPASS_ABBREV_TO_KMA_DEG: Map<String, Int> = mapOf(
        "N" to 360,
        "NNE" to 20,
        "NE" to 50,
        "ENE" to 70,
        "E" to 90,
        "ESE" to 110,
        "SE" to 140,
        "SSE" to 160,
        "S" to 180,
        "SSW" to 200,
        "SW" to 230,
        "WSW" to 250,
        "W" to 270,
        "WNW" to 290,
        "NW" to 320,
        "NNW" to 340,
    )

    private val DIRECTIONS = listOf(
        "N", "NNE", "NE", "ENE",
        "E", "ESE", "SE", "SSE",
        "S", "SSW", "SW", "WSW",
        "W", "WNW", "NW", "NNW",
    )

    fun to16Point(degrees: Int?, windSpeedMps: Double? = null): String {
        if (degrees == null) return "-"
        if (degrees == 99) return "불정"
        if (degrees == 0 && windSpeedMps != null && windSpeedMps < 0.3) return "무풍"
        val normalized = ((degrees % 360) + 360) % 360
        val index = (((normalized + 11.25) / 22.5).toInt()) % 16
        return DIRECTIONS[index]
    }

    fun mergedWindDirectionDegrees(apiVec: Int?, windSpeedMps: Double?, fallback: Int?): Int? = when {
        apiVec == 99 -> null
        apiVec == 0 && windSpeedMps != null && windSpeedMps < 0.3 -> null
        apiVec != null -> apiVec
        else -> fallback
    }

    fun referencePoints(): List<Pair<String, Int>> =
        listOf(
            "N" to 0,
            "NNE" to 20,
            "NE" to 50,
            "ENE" to 70,
            "E" to 90,
            "ESE" to 110,
            "SE" to 140,
            "SSE" to 160,
            "S" to 180,
            "SSW" to 200,
            "SW" to 230,
            "WSW" to 250,
            "W" to 270,
            "WNW" to 290,
            "NW" to 320,
            "NNW" to 340,
            "N" to 360,
        )
}
