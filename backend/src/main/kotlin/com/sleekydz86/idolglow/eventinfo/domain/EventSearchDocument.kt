package com.sleekydz86.idolglow.eventinfo.domain

import java.time.LocalDate

data class EventSearchDocument(
    val globalId: String,
    val title: String,
    val titles: Map<String, String>,
    val venue: String?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val status: String,
    val mergedInto: String?,
    val metaVersion: Long,
) {
    companion object {
        fun of(
            globalId: String,
            snapshot: EventMetaSnapshot,
            metaVersion: Long,
            translations: List<EventTranslation>,
        ): EventSearchDocument =
            EventSearchDocument(
                globalId = globalId,
                title = snapshot.title,
                titles =
                    translations
                        .filter { it.locale != EventLocale.KOREAN && it.title.isNotBlank() }
                        .associate { it.locale to it.title },
                venue = snapshot.venue,
                startDate = snapshot.startDate,
                endDate = snapshot.endDate,
                status = snapshot.status,
                mergedInto = snapshot.mergedInto,
                metaVersion = metaVersion,
            )
    }
}

data class EventSearchHit(
    val globalId: String,
    val title: String,
    val titles: Map<String, String>,
    val venue: String?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
)

enum class EventSearchIndexOutcome {
    INDEXED,
    STALE,
}
