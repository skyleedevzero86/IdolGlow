package com.sleekydz86.idolglow.subway.infrastructure.enrichment

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.sleekydz86.idolglow.subway.domain.SubwayPageEnrichment
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component
class ClasspathSubwayPageEnrichmentAdapter(
    private val objectMapper: ObjectMapper,
) {

    private val fileRoot: EnrichmentFileRoot by lazy(::readFile)

    fun loadClasspathEnrichment(
        lineId: String,
        @Suppress("UNUSED_PARAMETER") lineName: String,
        stationCd: String,
        stationDisplayName: String,
    ): SubwayPageEnrichment {
        val key = "$lineId:$stationCd"
        val ov = fileRoot.overrides[key]
        val def = fileRoot.defaultBlock
        return build(def, ov, stationDisplayName)
    }

    private fun readFile(): EnrichmentFileRoot {
        val res = ClassPathResource("subway/page-enrichments.json")
        check(res.exists()) { "classpath:subway/page-enrichments.json 파일이 필요합니다." }
        return res.inputStream.use { stream -> objectMapper.readValue(stream, EnrichmentFileRoot::class.java) }
    }

    private fun build(def: DefaultBlockDto, ov: OverrideBlockDto?, stationName: String): SubwayPageEnrichment {
        val titleRaw = when {
            ov?.summaryTitle != null -> ov.summaryTitle
            ov?.summaryTitleTemplate != null -> ov.summaryTitleTemplate
            def.summaryTitle != null -> def.summaryTitle
            else -> def.summaryTitleTemplate ?: "{stationName}"
        }
        val bulletsRaw = when {
            ov?.summaryBullets != null && ov.summaryBullets.isNotEmpty() -> ov.summaryBullets
            else -> def.summaryBullets
        }
        val learnRaw = when {
            ov?.learnMoreLabel != null -> ov.learnMoreLabel
            ov?.learnMoreLabelTemplate != null -> ov.learnMoreLabelTemplate
            def.learnMoreLabel != null -> def.learnMoreLabel
            else -> def.learnMoreLabelTemplate ?: "{stationName} 에 대해 더 알아보세요"
        }
        val learnUrlRaw = ov?.learnMoreUrl ?: def.learnMoreUrl
        val learnUrl = learnUrlRaw?.trim()?.takeIf { it.isNotEmpty() }?.let { tpl(it, stationName) }
        val radius = ov?.nearbyRadiusMeters ?: def.nearbyRadiusMeters
        val count = ov?.nearbyCount ?: def.nearbyCount
        val nearbyLabelRaw = ov?.nearbyLabel ?: def.nearbyLabel
        return SubwayPageEnrichment(
            summaryTitle = tpl(titleRaw, stationName),
            summaryBullets = bulletsRaw.map { tpl(it, stationName) },
            learnMoreLabel = tpl(learnRaw, stationName),
            learnMoreUrl = learnUrl?.takeIf { it.isNotEmpty() },
            nearbyRadiusMeters = radius,
            nearbyCount = count,
            nearbyLabel = tpl(nearbyLabelRaw, stationName),
        )
    }

    private fun tpl(s: String, stationName: String): String {
        val mapsQuery = URLEncoder.encode("${stationName}역", StandardCharsets.UTF_8)
        return s.replace("{stationName}", stationName)
            .replace("{stationNameForMaps}", mapsQuery)
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class EnrichmentFileRoot(
        @JsonProperty("default") val defaultBlock: DefaultBlockDto,
        val overrides: Map<String, OverrideBlockDto> = emptyMap(),
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class DefaultBlockDto(
        val summaryTitle: String? = null,
        val summaryTitleTemplate: String? = null,
        val summaryBullets: List<String> = emptyList(),
        val learnMoreLabel: String? = null,
        val learnMoreLabelTemplate: String? = null,
        val learnMoreUrl: String? = null,
        val nearbyRadiusMeters: Int = 2000,
        val nearbyCount: Int = 0,
        val nearbyLabel: String = "따릉이",
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class OverrideBlockDto(
        val summaryTitle: String? = null,
        val summaryTitleTemplate: String? = null,
        val summaryBullets: List<String>? = null,
        val learnMoreLabel: String? = null,
        val learnMoreLabelTemplate: String? = null,
        val learnMoreUrl: String? = null,
        val nearbyRadiusMeters: Int? = null,
        val nearbyCount: Int? = null,
        val nearbyLabel: String? = null,
    )
}
