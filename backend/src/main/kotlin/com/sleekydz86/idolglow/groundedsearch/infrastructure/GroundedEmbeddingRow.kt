package com.sleekydz86.idolglow.groundedsearch.infrastructure

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class GroundedEmbeddingRow(
    val index: Int = 0,
    val embedding: List<Double> = emptyList(),
)
