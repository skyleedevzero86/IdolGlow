package com.sleekydz86.idolglow.subway.infrastructure.catalog

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.sleekydz86.idolglow.subway.application.port.out.SubwayLineCatalogPort
import com.sleekydz86.idolglow.subway.domain.SubwayLine
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

@Component
class ClasspathSubwayLineCatalogAdapter(
    private val objectMapper: ObjectMapper,
) : SubwayLineCatalogPort {

    private val cached: List<SubwayLine> by lazy { readCatalogLines() }

    override fun loadAllLines(): List<SubwayLine> = cached

    private fun readCatalogLines(): List<SubwayLine> {
        val res = ClassPathResource("subway/catalog-lines.json")
        if (!res.exists()) {
            return emptyList()
        }
        val rows: List<LineJson> = res.inputStream.use { stream ->
            objectMapper.readValue(stream, object : TypeReference<List<LineJson>>() {})
        }
        return rows.map { SubwayLine(id = it.id, name = it.name, colorHex = it.colorHex) }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class LineJson(
        val id: String,
        val name: String,
        val colorHex: String,
    )
}
