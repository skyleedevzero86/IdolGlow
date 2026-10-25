package com.sleekydz86.idolglow.groundedsearch.adapter.web

import com.sleekydz86.idolglow.groundedsearch.application.GroundedReindexResult

data class GroundedReindexResponse(
    val embedded: Int,
    val skipped: Int,
) {
    companion object {
        fun from(result: GroundedReindexResult): GroundedReindexResponse =
            GroundedReindexResponse(embedded = result.embedded, skipped = result.skipped)
    }
}
