package com.sleekydz86.idolglow.groundedsearch.infrastructure

import com.fasterxml.jackson.annotation.JsonProperty

data class GroundedAnthropicRequest(
    val model: String,
    val messages: List<GroundedLlmMessage>,
    val system: String,
    @JsonProperty("max_tokens")
    val maxTokens: Int,
)
