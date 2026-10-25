package com.sleekydz86.idolglow.groundedsearch.application.port

import com.sleekydz86.idolglow.groundedsearch.domain.GroundedConditionDraft

interface ConditionExtractPort {
    fun extract(query: String): GroundedConditionDraft
}
