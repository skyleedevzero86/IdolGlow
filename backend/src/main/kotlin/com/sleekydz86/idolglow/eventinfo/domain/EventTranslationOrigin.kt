package com.sleekydz86.idolglow.eventinfo.domain

enum class EventTranslationOrigin(
    val visible: Boolean,
) {
    PROVIDER(true),
    MANUAL(true),
    MACHINE(false),
    ;

    companion object {
        val VISIBLE: List<EventTranslationOrigin> = entries.filter { it.visible }
        val HIDDEN: List<EventTranslationOrigin> = entries.filterNot { it.visible }
    }
}
