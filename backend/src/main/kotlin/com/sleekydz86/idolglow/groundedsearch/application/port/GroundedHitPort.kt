package com.sleekydz86.idolglow.groundedsearch.application.port

import com.sleekydz86.idolglow.groundedsearch.domain.FusedRank
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedSearchHit

interface GroundedHitPort {
    fun read(fused: List<FusedRank>): List<GroundedSearchHit>
}
