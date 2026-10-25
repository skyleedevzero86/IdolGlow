package com.sleekydz86.idolglow.groundedsearch.application.port

import com.sleekydz86.idolglow.groundedsearch.domain.GroundedSearchHit

interface RerankPort {
    fun rerank(query: String, hits: List<GroundedSearchHit>): List<GroundedSearchHit>
}
