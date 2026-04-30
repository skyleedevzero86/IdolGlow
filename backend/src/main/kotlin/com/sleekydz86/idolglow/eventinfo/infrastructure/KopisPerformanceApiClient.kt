package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.domain.FestivalEvent
import com.sleekydz86.idolglow.eventinfo.domain.KopisAreaStat
import com.sleekydz86.idolglow.global.infrastructure.config.KopisApiProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriUtils
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import java.nio.charset.StandardCharsets
import javax.xml.parsers.DocumentBuilderFactory

@Component
class KopisPerformanceApiClient(
    private val webClient: WebClient,
    private val kopisApiProperties: KopisApiProperties,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun listPerformances(
        stDate: String,
        edDate: String,
        page: Int,
        rows: Int,
        signguCode: String? = "11",
        prfState: String? = "02",
    ): List<FestivalEvent> {
        val key = kopisApiProperties.serviceKey.trim()
        if (key.isEmpty()) return emptyList()
        val xml = callEndpoint(
            path = "pblprfr",
            query = buildMap {
                put("service", key)
                put("stdate", stDate)
                put("eddate", edDate)
                put("cpage", page.coerceAtLeast(1).toString())
                put("rows", rows.coerceIn(1, 100).toString())
                signguCode?.takeIf { it.isNotBlank() }?.let { put("signgucode", it) }
                prfState?.takeIf { it.isNotBlank() }?.let { put("prfstate", it) }
            },
        ) ?: return emptyList()
        return parseRows(xml, "db").mapNotNull { row ->
            val id = row.text("mt20id") ?: return@mapNotNull null
            val title = row.text("prfnm") ?: return@mapNotNull null
            FestivalEvent(
                contentId = id,
                title = title,
                address = row.text("fcltynm"),
                eventStartDate = normalizeDate(row.text("prfpdfrom")),
                eventEndDate = normalizeDate(row.text("prfpdto")),
                thumbnailImageUrl = row.text("poster"),
                imageUrl = row.text("poster"),
                mapX = null,
                mapY = null,
                phone = null,
                detailUrl = null,
                category = row.text("genrenm"),
                synopsis = null,
                source = "KOPIS_PBLPRFR",
            )
        }
    }

    fun detailPerformance(mt20id: String): FestivalEvent? {
        val key = kopisApiProperties.serviceKey.trim()
        if (key.isEmpty()) return null
        val xml = callEndpoint(
            path = "pblprfr/${UriUtils.encodePath(mt20id.trim(), StandardCharsets.UTF_8)}",
            query = mapOf("service" to key),
        ) ?: return null
        val row = parseRows(xml, "db").firstOrNull() ?: return null
        val id = row.text("mt20id") ?: return null
        val title = row.text("prfnm") ?: return null
        return FestivalEvent(
            contentId = id,
            title = title,
            address = row.text("fcltynm"),
            eventStartDate = normalizeDate(row.text("prfpdfrom")),
            eventEndDate = normalizeDate(row.text("prfpdto")),
            thumbnailImageUrl = row.text("poster"),
            imageUrl = row.text("poster"),
            mapX = null,
            mapY = null,
            phone = null,
            detailUrl = row.firstText("relateurl"),
            category = row.text("genrenm"),
            synopsis = row.text("sty"),
            source = "KOPIS_PBLPRFR",
            cast = row.text("prfcast"),
            runningTime = row.text("prfruntime"),
            age = row.text("prfage"),
            bookingPlaces = row.text("entrpsnm"),
            introImageUrls = row.texts("styurl"),
        )
    }

    fun listFestivals(
        stDate: String,
        edDate: String,
        page: Int,
        rows: Int,
    ): List<FestivalEvent> {
        val key = kopisApiProperties.serviceKey.trim()
        if (key.isEmpty()) return emptyList()
        val xml = callEndpoint(
            path = "prffest",
            query = mapOf(
                "service" to key,
                "stdate" to stDate,
                "eddate" to edDate,
                "cpage" to page.coerceAtLeast(1).toString(),
                "rows" to rows.coerceIn(1, 100).toString(),
            ),
        ) ?: return emptyList()
        return parseRows(xml, "db").mapNotNull { row ->
            val id = row.text("mt20id") ?: return@mapNotNull null
            val title = row.text("prfnm") ?: return@mapNotNull null
            FestivalEvent(
                contentId = id,
                title = title,
                address = row.text("fcltynm"),
                eventStartDate = normalizeDate(row.text("prfpdfrom")),
                eventEndDate = normalizeDate(row.text("prfpdto")),
                thumbnailImageUrl = row.text("poster"),
                imageUrl = row.text("poster"),
                mapX = null,
                mapY = null,
                phone = null,
                detailUrl = null,
                category = row.text("genrenm"),
                synopsis = null,
                source = "KOPIS_PRFFEST",
            )
        }
    }

    fun areaStats(stDate: String, edDate: String): List<KopisAreaStat> {
        val key = kopisApiProperties.serviceKey.trim()
        if (key.isEmpty()) return emptyList()
        val xml = callEndpoint(
            path = "prfstsArea",
            query = mapOf("service" to key, "stdate" to stDate, "eddate" to edDate),
        ) ?: return emptyList()
        return parseRows(xml, "prfst").mapNotNull { row ->
            val area = row.text("area") ?: return@mapNotNull null
            KopisAreaStat(
                area = area,
                fcltycnt = row.text("fcltycnt")?.toLongOrNull(),
                prfplccnt = row.text("prfplccnt")?.toLongOrNull(),
                seatcnt = row.text("seatcnt")?.toLongOrNull(),
                prfcnt = row.text("prfcnt")?.toLongOrNull(),
                prfprocnt = row.text("prfprocnt")?.toLongOrNull(),
                prfdtcnt = row.text("prfdtcnt")?.toLongOrNull(),
                nmrs = row.text("nmrs")?.toLongOrNull(),
                nmrcancl = row.text("nmrcancl")?.toLongOrNull(),
                totnmrs = row.text("totnmrs")?.toLongOrNull(),
                amount = row.text("amount")?.toLongOrNull(),
            )
        }
    }

    private fun callEndpoint(path: String, query: Map<String, String>): String? {
        val queryString = query.entries.joinToString("&") { (k, v) ->
            "${UriUtils.encodeQueryParam(k, StandardCharsets.UTF_8)}=${UriUtils.encodeQueryParam(v, StandardCharsets.UTF_8)}"
        }
        val url = "${kopisApiProperties.baseUrl.trimEnd('/')}/$path?$queryString"
        return runCatching {
            webClient.get().uri(url).retrieve().bodyToMono(String::class.java).block()
        }.onFailure {
            log.warn("KOPIS(공연) 호출 실패. path={}, message={}", path, it.message)
        }.getOrNull()
    }

    private fun parseRows(xml: String, rowTag: String): List<Element> {
        return runCatching {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = false
            val builder = factory.newDocumentBuilder()
            val document = builder.parse(InputSource(StringReader(xml)))
            val nodes = document.getElementsByTagName(rowTag)
            (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }
        }.getOrElse {
            log.warn("KOPIS(공연) XML 파싱 실패: {}", it.message)
            emptyList()
        }
    }

    private fun normalizeDate(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) return null
        val yyyymmdd = value.replace(".", "").replace("-", "")
        return if (yyyymmdd.length == 8 && yyyymmdd.all { it.isDigit() }) yyyymmdd else null
    }
}

private fun Element.text(tagName: String): String? {
    val nodes = this.getElementsByTagName(tagName)
    if (nodes.length == 0) return null
    return nodes.item(0)?.textContent?.trim()?.takeIf { it.isNotEmpty() }
}

private fun Element.firstText(tagName: String): String? = text(tagName)

private fun Element.texts(tagName: String): List<String> {
    val nodes = this.getElementsByTagName(tagName)
    if (nodes.length == 0) return emptyList()
    return (0 until nodes.length)
        .mapNotNull { idx -> nodes.item(idx)?.textContent?.trim()?.takeIf { it.isNotEmpty() } }
        .distinct()
}
