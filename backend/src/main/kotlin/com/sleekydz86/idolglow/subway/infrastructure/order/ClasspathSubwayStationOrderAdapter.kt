package com.sleekydz86.idolglow.subway.infrastructure.order

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.sleekydz86.idolglow.subway.application.port.out.SubwayStationOrderPort
import com.sleekydz86.idolglow.subway.domain.SubwayStationStop
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class ClasspathSubwayStationOrderAdapter(
    private val objectMapper: ObjectMapper,
) : SubwayStationOrderPort {

    private val manifest: Map<String, String> by lazy(::readManifest)
    private val cache = ConcurrentHashMap<String, List<SubwayStationStop>>()

    override fun orderedStops(lineId: String): List<SubwayStationStop> =
        cache.computeIfAbsent(lineId) { id ->
            val path = manifest[id] ?: return@computeIfAbsent emptyList()
            loadStops(path)
        }

    private fun readManifest(): Map<String, String> {
        val res = ClassPathResource("subway/station-order-manifest.json")
        if (!res.exists()) {
            return emptyMap()
        }
        return res.inputStream.use { stream ->
            objectMapper.readValue(stream, object : TypeReference<Map<String, String>>() {})
        }
    }

    private fun loadStops(classpath: String): List<SubwayStationStop> {
        val res = ClassPathResource(classpath)
        if (!res.exists()) {
            return emptyList()
        }
        val rows: List<StopJson> = res.inputStream.use { stream ->
            objectMapper.readValue(stream, object : TypeReference<List<StopJson>>() {})
        }
        return rows.map { SubwayStationStop(code = it.cd, name = it.nm, frCode = it.fr) }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class StopJson(
        val cd: String,
        val nm: String,
        val fr: String,
    )
}
