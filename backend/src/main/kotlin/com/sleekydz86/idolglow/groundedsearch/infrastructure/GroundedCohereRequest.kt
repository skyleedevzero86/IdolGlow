package com.sleekydz86.idolglow.groundedsearch.infrastructure

data class GroundedCohereRequest(
    val model: String,
    val query: String,
    val documents: List<String>,
)
