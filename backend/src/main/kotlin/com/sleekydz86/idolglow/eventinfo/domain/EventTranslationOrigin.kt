package com.sleekydz86.idolglow.eventinfo.domain

enum class EventTranslationOrigin {
    MACHINE,
    PROVIDER,
    MANUAL,
    ;

    fun canReplace(existing: EventTranslationOrigin): Boolean =
        ordinal > existing.ordinal || (this == existing && this != MACHINE)

    companion object {
        val REVIEWED: List<EventTranslationOrigin> = listOf(PROVIDER, MANUAL)
    }
}
