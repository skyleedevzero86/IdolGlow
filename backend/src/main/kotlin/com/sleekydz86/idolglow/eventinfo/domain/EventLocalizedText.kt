package com.sleekydz86.idolglow.eventinfo.domain

data class EventLocalizedText(
    val title: String,
    val synopsis: String?,
    val locale: String,
    val origin: EventTranslationOrigin? = null,
)

data class EventTextLookup(
    val source: String,
    val sourceId: String,
    val title: String,
    val synopsis: String?,
)
