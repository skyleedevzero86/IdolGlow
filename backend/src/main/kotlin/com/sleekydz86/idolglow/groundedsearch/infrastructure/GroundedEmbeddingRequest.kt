package com.sleekydz86.idolglow.groundedsearch.infrastructure

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class GroundedEmbeddingRequest(
    val model: String,
    val input: List<String>,
)
