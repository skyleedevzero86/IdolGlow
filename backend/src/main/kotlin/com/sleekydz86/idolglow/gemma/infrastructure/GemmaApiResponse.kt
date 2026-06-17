package com.sleekydz86.idolglow.gemma.infrastructure

import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import com.sleekydz86.idolglow.global.infrastructure.config.GemmaProperties
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

data class GemmaApiResponse(
    val httpStatus: Int,
    val json: JsonNode?,
    val rawBody: String?,
    val error: Throwable? = null,
) {
    val isSuccess2xx: Boolean get() = httpStatus in 200..299
}
