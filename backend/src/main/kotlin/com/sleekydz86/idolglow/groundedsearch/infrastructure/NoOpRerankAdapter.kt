package com.sleekydz86.idolglow.groundedsearch.infrastructure

import com.sleekydz86.idolglow.groundedsearch.application.port.RerankPort
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedSearchHit
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    name = ["app.grounded-search.rerank.enabled"],
    havingValue = "false",
    matchIfMissing = true,
)
class NoOpRerankAdapter : RerankPort {
    override fun rerank(query: String, hits: List<GroundedSearchHit>): List<GroundedSearchHit> = hits
}
