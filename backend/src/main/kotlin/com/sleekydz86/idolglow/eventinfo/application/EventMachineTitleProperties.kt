package com.sleekydz86.idolglow.eventinfo.application

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.event-machine-title")
data class EventMachineTitleProperties(
    val enabled: Boolean = true,
    val maxPerLocale: Int = 100,
) {
    init {
        require(maxPerLocale > 0) { "AI 공연 제목 번역의 언어당 최대 건수는 1 이상이어야 합니다. maxPerLocale=$maxPerLocale" }
    }
}
