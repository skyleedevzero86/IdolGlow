package com.sleekydz86.idolglow.eventinfo.domain

enum class EventTranslationOrigin(
    private val priority: Int,
) {
    PROVIDER(2),
    MANUAL(3),
    MACHINE(1),
    ;

    fun canReplace(existing: EventTranslationOrigin): Boolean =
        priority > existing.priority || (this == existing && this != MACHINE)

    companion object {
        val REVIEWED: List<EventTranslationOrigin> = listOf(PROVIDER, MANUAL)
    }
}
