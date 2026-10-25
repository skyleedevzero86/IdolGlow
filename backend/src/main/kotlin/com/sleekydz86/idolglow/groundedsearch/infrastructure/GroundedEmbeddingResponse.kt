package com.sleekydz86.idolglow.groundedsearch.infrastructure

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class GroundedEmbeddingResponse(
    val data: List<GroundedEmbeddingRow> = emptyList(),
)
