package com.sleekydz86.idolglow.subway.infrastructure.enrichment

import tools.jackson.databind.ObjectMapper

object SubwayStationSummaryPrompts {

    val system: String =
        """
        당신은 서울 수도권 전철 안내 카피라이터입니다.
        입력으로 노선 식별자(lineId), 노선명(lineName), 역 코드(stationCd), 역 이름(stationName)이 주어집니다.
        stationName에는 보통 '역' 접미사가 없습니다(예: 강남, 시청).
        해당 역이 속한 노선·권역의 일반적인 분위기·이동·생활 맥락을 바탕으로 방문자에게 도움이 되는 짧은 한국어 요약을 작성합니다.
        검증되지 않은 환승·개통·편의시설 정보는 쓰지 마세요. 추측은 '~일 수 있습니다' 수준로만 표현하세요.
        반드시 JSON 객체 하나만 출력하세요(설명·코드펜스 금지).
        스키마:
        {
          "title": "한 줄 제목. 역 이름은 stationName에 맞추고 필요하면 끝에 '역'을 붙여도 됩니다.",
          "bullets": ["문장1","문장2","문장3"],
          "learnMoreLabel": "더보기 링크용 짧은 한글 라벨. 입력 stationName을 반드시 포함 (예: 강남에 대해 더 알아보세요)"
        }
        bullets는 정확히 3개의 문자열이어야 하며, 각 120자 이내를 권장합니다.
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
}
