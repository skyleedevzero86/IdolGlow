package com.sleekydz86.idolglow.eventinfo.domain

object EventTranslationFallback {
    fun pick(
        requested: String,
        rows: List<EventTranslation>,
    ): EventTranslation? =
        EventLocale.order(requested).firstNotNullOfOrNull { locale ->
            rows.find { it.locale == locale }
        }
}
