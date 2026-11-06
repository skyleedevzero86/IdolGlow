package com.sleekydz86.idolglow.subway.infrastructure.enrichment

import tools.jackson.databind.ObjectMapper

object SubwayStationSummaryPrompts {
    val system: String =
        """
        You write short visitor tips for Seoul metropolitan subway stations.
        Input JSON has lineId, lineName, stationCd and stationName.
        Describe the general atmosphere, transit and daily-life context of the station's line and area.
        Do not state unverified transfers, opening dates or facilities. Hedge guesses with "may".
        Do not write the station name or any other place name; the page already shows it.
        Write in plain English. Output exactly one JSON object, no code fence:
        {"bullets": ["sentence 1", "sentence 2", "sentence 3"]}
        bullets must have exactly 3 sentences, each under 120 characters.
        """.trimIndent()

    val translateToKorean: String =
        """
        Translate each sentence in the input JSON "bullets" array into natural, polite Korean.
        Use Hangul only. Never use Chinese characters, Japanese kana or English words.
        Keep the same number of sentences in the same order. Do not add new information.
        Output exactly one JSON object, no code fence: {"bullets": ["...", "...", "..."]}
        """.trimIndent()

    fun userJson(
        objectMapper: ObjectMapper,
        lineId: String,
        lineName: String,
        stationCd: String,
        stationDisplayName: String,
    ): String =
        objectMapper.writeValueAsString(
            mapOf(
                "lineId" to lineId,
                "lineName" to lineName,
                "stationCd" to stationCd,
                "stationName" to stationDisplayName,
            ),
        )

    fun bulletsJson(
        objectMapper: ObjectMapper,
        bullets: List<String>,
    ): String = objectMapper.writeValueAsString(mapOf("bullets" to bullets))
}
