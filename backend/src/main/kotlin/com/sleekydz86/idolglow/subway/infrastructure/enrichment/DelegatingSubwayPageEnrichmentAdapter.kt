package com.sleekydz86.idolglow.subway.infrastructure.enrichment

import com.sleekydz86.idolglow.subway.application.port.out.SubwayPageEnrichmentPort
import com.sleekydz86.idolglow.subway.domain.SubwayPageEnrichment
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component
class DelegatingSubwayPageEnrichmentAdapter(
    private val classpath: ClasspathSubwayPageEnrichmentAdapter,
    private val subwayStationSummaryLlmRouter: SubwayStationSummaryLlmRouter,
) : SubwayPageEnrichmentPort {

    override fun loadFor(
        lineId: String,
        lineName: String,
        stationCd: String,
        stationDisplayName: String,
    ): SubwayPageEnrichment {
        val base = classpath.loadClasspathEnrichment(lineId, lineName, stationCd, stationDisplayName)
        val llm = subwayStationSummaryLlmRouter.generateSummary(
            lineId = lineId,
            lineName = lineName,
            stationCd = stationCd,
            stationDisplayName = stationDisplayName,
        ) ?: return base

        val title = llm.title?.takeIf { it.isNotEmpty() } ?: base.summaryTitle
        val llmBullets = llm.bullets ?: emptyList()
        val bullets = when {
            llmBullets.size >= 3 -> llmBullets.take(3)
            llmBullets.isEmpty() -> base.summaryBullets
            else -> {
                val merged = llmBullets.toMutableList()
                for (b in base.summaryBullets) {
                    if (merged.size >= 3) break
                    if (b !in merged) merged.add(b)
                }
                merged.take(3)
            }
        }

        val learnLabel = llm.learnMoreLabel?.takeIf { it.isNotEmpty() } ?: base.learnMoreLabel
        val mapsUrl = googleMapsStationUrl(stationDisplayName)

        return base.copy(
            summaryTitle = title,
            summaryBullets = bullets,
            learnMoreLabel = learnLabel,
            learnMoreUrl = mapsUrl,
        )
    }

    private fun googleMapsStationUrl(stationName: String): String {
        val q = URLEncoder.encode("${stationName}역", StandardCharsets.UTF_8)
        return "https://www.google.com/maps/search/?api=1&query=$q"
    }
}
