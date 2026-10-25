package com.sleekydz86.idolglow.groundedsearch.domain

data class GroundedCitationPrompt(
    val label: String,
    val chunkId: Long,
    val linkKey: String,
    val content: String,
)
