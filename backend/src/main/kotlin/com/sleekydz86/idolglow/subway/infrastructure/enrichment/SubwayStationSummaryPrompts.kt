package com.sleekydz86.idolglow.subway.infrastructure.enrichment

import com.sleekydz86.idolglow.subway.domain.SubwayStationFacts
import tools.jackson.databind.ObjectMapper

object SubwayStationSummaryPrompts {
    val system: String =
        """
        You write 3 short sentences for one Seoul subway station page.
        Use ONLY the facts in the input JSON. Never add a place, landmark, history or number that is not in the input.
        Copy place and station names exactly as given. Write natural polite Korean sentences ending with "~습니다" or "~요".
        Never use Chinese characters, Japanese or English words.
        Example output for other facts:
        {"bullets": ["3호선 역으로 이웃 역은 안국과 종로3가입니다.", "약 300m 거리에 창덕궁이, 약 450m 거리에 북촌한옥마을이 있습니다.", "인사동거리도 약 600m로 걸어서 둘러볼 수 있습니다."]}
        Output exactly one JSON object: {"bullets": ["...", "...", "..."]}
        1st sentence: line, transfer lines if any, neighboring stations. 2nd and 3rd: nearby places with distance.
        """.trimIndent()

    fun userJson(
        objectMapper: ObjectMapper,
        facts: SubwayStationFacts,
    ): String =
        objectMapper.writeValueAsString(
            mapOf(
                "station" to facts.station,
                "line" to facts.line,
                "transferLines" to facts.transferLines,
                "neighborStations" to facts.neighborStations,
                "nearbyPlaces" to
                    facts.nearbyPlaces.map {
                        mapOf("name" to it.name, "type" to it.category, "meters" to it.distanceMeters)
                    },
            ),
        )
}
