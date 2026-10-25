package com.sleekydz86.idolglow.groundedsearch.application

data class GroundedPrepareResult(
    val toEmbed: List<GroundedChunkToEmbed>,
    val skipped: Int,
)
