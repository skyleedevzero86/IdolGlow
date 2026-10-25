package com.sleekydz86.idolglow.groundedsearch.application.port

import com.sleekydz86.idolglow.groundedsearch.domain.GroundedRetrieval
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedScope

interface GroundedRetrievePort {
    fun retrieve(query: String, scope: GroundedScope): GroundedRetrieval
}
