package com.sleekydz86.idolglow.eventinfo.application.port.out

import com.sleekydz86.idolglow.eventinfo.domain.EventTranslationAttempt

interface EventTranslationAttemptPort {
    fun find(
        globalId: String,
        locale: String,
    ): EventTranslationAttempt?

    fun save(attempt: EventTranslationAttempt): EventTranslationAttempt

    fun delete(
        globalId: String,
        locale: String,
    )
}
