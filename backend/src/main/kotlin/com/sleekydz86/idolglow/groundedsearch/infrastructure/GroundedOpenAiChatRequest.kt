package com.sleekydz86.idolglow.groundedsearch.infrastructure

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class GroundedOpenAiChatRequest(
    val model: String,
    val messages: List<GroundedLlmMessage>,
    @JsonProperty("response_format")
    val responseFormat: GroundedResponseFormat? = null,
)
