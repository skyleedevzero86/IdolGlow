package com.sleekydz86.idolglow.eventinfo.application.port.out

import com.sleekydz86.idolglow.eventinfo.domain.EventSearchDocument
import com.sleekydz86.idolglow.eventinfo.domain.EventSearchHit
import com.sleekydz86.idolglow.eventinfo.domain.EventSearchIndexOutcome

interface EventSearchIndexPort {
    fun index(
        document: EventSearchDocument,
        overwriteSameVersion: Boolean,
    ): EventSearchIndexOutcome

    fun search(
        query: String,
        size: Int,
    ): List<EventSearchHit>
}
