package com.sleekydz86.idolglow.glowweather.domain

object WindDirection {
    private val DIRECTIONS = listOf(
        "N", "NNE", "NE", "ENE",
        "E", "ESE", "SE", "SSE",
        "S", "SSW", "SW", "WSW",
        "W", "WNW", "NW", "NNW",
    )

    fun to16Point(degrees: Int?): String {
        if (degrees == null) return "-"
        val normalized = ((degrees % 360) + 360) % 360
        val index = (((normalized + 11.25) / 22.5).toInt()) % 16
        return DIRECTIONS[index]
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
