package com.sleekydz86.idolglow.eventinfo.domain

data class EventMetaMessage(
    val eventId: String = "",
    val eventType: String = "",
    val globalId: String = "",
    val metaVersion: Long = 0,
    val schemaVersion: Int = SCHEMA_VERSION,
    val occurredAt: String = "",
    val changedFields: List<String> = emptyList(),
    val snapshot: EventMetaSnapshot? = null,
) {
    fun alertWorthy(): Boolean = changedFields.any { it in EventMetaSnapshot.ALERT_FIELDS }

    companion object {
        const val SCHEMA_VERSION: Int = 1
    }
}
