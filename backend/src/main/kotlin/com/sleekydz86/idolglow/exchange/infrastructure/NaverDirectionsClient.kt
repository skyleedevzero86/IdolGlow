package com.sleekydz86.idolglow.exchange.infrastructure

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.sleekydz86.idolglow.global.infrastructure.config.NaverDirectionsProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.Duration
import kotlin.math.ceil

@Component
class NaverDirectionsClient(
    private val webClient: WebClient,
    private val properties: NaverDirectionsProperties,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun drivingDurationMinutes(startLng: Double, startLat: Double, goalLng: Double, goalLat: Double): Int? {
        if (!properties.enabled) {
            return null
        }
        val id = properties.clientId.trim()
        val secret = properties.clientSecret.trim()
        if (id.isEmpty() || secret.isEmpty()) {
            return null
        }
        val base = properties.baseUrl.trim().trimEnd('/')
        val optionKey = properties.routeOption.trim().ifEmpty { "traoptimal" }
        val uri: URI = UriComponentsBuilder.fromUriString("$base/map-direction/v1/driving")
            .queryParam("start", "$startLng,$startLat")
            .queryParam("goal", "$goalLng,$goalLat")
            .queryParam("option", optionKey)
            .build()
            .encode()
            .toUri()

        val body: String = try {
            webClient.get()
                .uri(uri)
                .header("x-ncp-apigw-api-key-id", id)
                .header("x-ncp-apigw-api-key", secret)
                .retrieve()
                .bodyToMono(String::class.java)
                .block(Duration.ofSeconds(12))
        } catch (e: Exception) {
            log.warn("Naver Directions request failed: {}", e.message)
            return null
        } ?: return null

        val ms = parseDurationMs(body, optionKey) ?: return null
        if (ms <= 0L) {
            return 0
        }
        return ceil(ms / 60_000.0).toInt().coerceAtLeast(1)
    }

    private fun parseDurationMs(json: String, optionKey: String): Long? {
        val root: JsonNode = try {
            objectMapper.readTree(json)
        } catch (e: Exception) {
            log.warn("Naver Directions JSON parse failed: {}", e.message)
            return null
        }
        val code = root["code"]?.asInt() ?: return null
        if (code != 0) {
            log.debug("Naver Directions code={} message={}", code, root["message"]?.asText())
            return null
        }
        val routeNode = root["route"] ?: return null
        val optionNode = routeNode[optionKey] ?: return null
        if (!optionNode.isArray || optionNode.isEmpty) {
            return null
        }
        val summary = optionNode[0]["summary"] ?: return null
        val duration = summary["duration"] ?: return null
        return duration.asLong()
    }
}
