package com.sleekydz86.idolglow.groundedsearch.infrastructure

import com.sleekydz86.idolglow.groundedsearch.application.port.RerankPort
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedSearchHit
import org.springframework.stereotype.Component

@Component
class NoOpRerankAdapter : RerankPort {
    override fun rerank(hits: List<GroundedSearchHit>): List<GroundedSearchHit> = hits
}
