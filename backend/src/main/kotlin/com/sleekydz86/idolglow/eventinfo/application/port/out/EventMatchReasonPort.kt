package com.sleekydz86.idolglow.eventinfo.application.port.out

import com.sleekydz86.idolglow.eventinfo.domain.EventMatchFacts

fun interface EventMatchReasonPort {
    fun reason(facts: EventMatchFacts): String
}
