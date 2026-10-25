package com.sleekydz86.idolglow.groundedsearch.domain

data class GroundedChunkDraft(
    val sourceType: RagSourceType,
    val sourceId: Long,
    val linkKey: String,
    val content: String,
)
