package com.sleekydz86.idolglow.eventinfo.domain

object EventTranslationFallback {
    fun pick(
        requested: String,
        rows: List<EventTranslation>,
    ): EventTranslation? {
        val visible = rows.filter { it.origin != EventTranslationOrigin.MACHINE }
        return EventLocale.order(requested).firstNotNullOfOrNull { locale ->
            visible.find { it.locale == locale }
        }
    }
}
