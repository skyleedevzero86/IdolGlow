package com.sleekydz86.idolglow.eventinfo.domain

import java.time.LocalDate

data class EventMetaSnapshot(
    val title: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val venue: String? = null,
    val status: String = "",
    val mergedInto: String? = null,
) {
    fun changedFields(after: EventMetaSnapshot): Set<String> =
        buildSet {
            if (title != after.title) add(FIELD_TITLE)
            if (startDate != after.startDate) add(FIELD_START_DATE)
            if (endDate != after.endDate) add(FIELD_END_DATE)
            if (venue != after.venue) add(FIELD_VENUE)
            if (status != after.status || mergedInto != after.mergedInto) add(FIELD_STATUS)
        }

    companion object {
        const val FIELD_TITLE: String = "title"
        const val FIELD_START_DATE: String = "startDate"
        const val FIELD_END_DATE: String = "endDate"
        const val FIELD_VENUE: String = "venue"
        const val FIELD_STATUS: String = "status"
        const val FIELD_TRANSLATION: String = "translation"
        const val FIELD_SESSIONS: String = "sessions"
        val ALERT_FIELDS: Set<String> = setOf(FIELD_START_DATE, FIELD_END_DATE, FIELD_VENUE, FIELD_STATUS)
    }
}
