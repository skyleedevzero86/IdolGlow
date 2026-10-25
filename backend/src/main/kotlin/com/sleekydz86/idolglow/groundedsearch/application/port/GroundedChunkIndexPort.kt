package com.sleekydz86.idolglow.groundedsearch.application.port

import com.sleekydz86.idolglow.groundedsearch.application.GroundedChunkToEmbed
import com.sleekydz86.idolglow.groundedsearch.application.GroundedPrepareResult
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedChunkDraft

interface GroundedChunkIndexPort {
    fun prepare(drafts: List<GroundedChunkDraft>, model: String): GroundedPrepareResult

    fun saveEmbeddings(rows: List<GroundedChunkToEmbed>, vectors: List<FloatArray>, model: String)
}
