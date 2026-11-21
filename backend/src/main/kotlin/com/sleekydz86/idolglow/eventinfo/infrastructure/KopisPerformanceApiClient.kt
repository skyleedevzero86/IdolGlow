package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.domain.FestivalEvent
import com.sleekydz86.idolglow.eventinfo.domain.KopisAreaStat
import com.sleekydz86.idolglow.global.config.KopisApiProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.util.UriUtils
import org.w3c.dom.Element
import org.xml.sax.InputSource
import reactor.core.publisher.Mono
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
        val xml =
            callEndpoint(
                path = "pblprfr",
                query =
                    buildMap {
                        put("service", key)
                        put("stdate", stDate)
                        put("eddate", edDate)
                        put("cpage", page.coerceAtLeast(1).toString())
                        put("rows", rows.coerceIn(1, 100).toString())
                        signguCode?.takeIf { it.isNotBlank() }?.let { put("signgucode", it) }
                        prfState?.takeIf { it.isNotBlank() }?.let { put("prfstate", it) }
                    },
            ) ?: return emptyList()
        return parseRows(xml, "db").mapNotNull { it.toListEvent("KOPIS_PBLPRFR") }
    }

    fun detailPerformance(mt20id: String): FestivalEvent? {
        val key = kopisApiProperties.serviceKey.trim()
        if (key.isEmpty()) return null
        val xml =
            callEndpoint(
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
        val xml =
            callEndpoint(
                path = "prffest",
                query =
                    mapOf(
                        "service" to key,
                        "stdate" to stDate,
                        "eddate" to edDate,
                        "cpage" to page.coerceAtLeast(1).toString(),
                        "rows" to rows.coerceIn(1, 100).toString(),
                    ),
            ) ?: return emptyList()
        return parseRows(xml, "db").mapNotNull { it.toListEvent("KOPIS_PRFFEST") }
    }

    fun areaStats(
        stDate: String,
        edDate: String,
    ): List<KopisAreaStat> {
        val key = kopisApiProperties.serviceKey.trim()
        if (key.isEmpty()) return emptyList()
        val xml =
            callEndpoint(
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

    fun listPerformancesForBatch(
        stDate: String,
        edDate: String,
        page: Int,
        rows: Int,
    ): List<FestivalEvent> =
        batchRows("pblprfr", periodQuery(stDate, edDate, page, rows))
            .mapNotNull { it.toListEvent("KOPIS_PBLPRFR") }

    fun listFestivalsForBatch(
        stDate: String,
        edDate: String,
        page: Int,
        rows: Int,
    ): List<FestivalEvent> =
        batchRows("prffest", periodQuery(stDate, edDate, page, rows))
            .mapNotNull { it.toListEvent("KOPIS_PRFFEST") }

    private fun batchRows(
        path: String,
        query: Map<String, String>,
    ): List<Element> {
        val key = kopisApiProperties.serviceKey.trim()
        check(key.isNotEmpty()) { "KOPIS API 인증키가 비어 있습니다." }
        val rows = parseXmlRows(requestXml(path, query + ("service" to key)), "db")
        val error = rows.firstOrNull { it.text("returncode") != null } ?: return rows
        val code = error.text("returncode")
        check(code == NO_DATA_CODE) { "KOPIS 오류 응답. path=$path returncode=$code errmsg=${error.text("errmsg")}" }
        return emptyList()
    }

    private fun callEndpoint(
        path: String,
        query: Map<String, String>,
    ): String? =
        runCatching { requestXml(path, query) }
            .onFailure { log.warn("KOPIS(공연) 호출 실패. path={}, message={}", path, it.message) }
            .getOrNull()

    private fun requestXml(
        path: String,
        query: Map<String, String>,
    ): String {
        val queryString =
            query.entries.joinToString("&") { (k, v) ->
                "${UriUtils.encodeQueryParam(k, StandardCharsets.UTF_8)}=${UriUtils.encodeQueryParam(v, StandardCharsets.UTF_8)}"
            }
        val url = "${kopisApiProperties.baseUrl.trimEnd('/')}/$path?$queryString"
        val body =
            webClient
                .get()
                .uri(url)
                .retrieve()
                .onStatus({ it.isError }) { response ->
                    Mono.error(IllegalStateException("KOPIS 호출 실패. path=$path status=${response.statusCode().value()}"))
                }.bodyToMono(String::class.java)
                .onErrorMap(WebClientRequestException::class.java) {
                    IllegalStateException("KOPIS 연결 실패. path=$path")
                }.block()
        return checkNotNull(body) { "KOPIS 응답 본문이 비어 있습니다. path=$path" }
    }

    private fun parseRows(
        xml: String,
        rowTag: String,
    ): List<Element> =
        runCatching { parseXmlRows(xml, rowTag) }
            .getOrElse {
                log.warn("KOPIS(공연) XML 파싱 실패: {}", it.message)
                emptyList()
            }
}

private const val MAX_LIST_ROWS: Int = 100
private const val NO_DATA_CODE: String = "04"
private const val COMPACT_DATE_LENGTH: Int = 8

private fun periodQuery(
    stDate: String,
    edDate: String,
    page: Int,
    rows: Int,
): Map<String, String> =
    mapOf(
        "stdate" to stDate,
        "eddate" to edDate,
        "cpage" to page.coerceAtLeast(1).toString(),
        "rows" to rows.coerceIn(1, MAX_LIST_ROWS).toString(),
    )

private fun parseXmlRows(
    xml: String,
    rowTag: String,
): List<Element> {
    val factory = DocumentBuilderFactory.newInstance()
    factory.isNamespaceAware = false
    val document = factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
    val nodes = document.getElementsByTagName(rowTag)
    return (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }
}

private fun Element.toListEvent(sourceName: String): FestivalEvent? {
    val id = text("mt20id")
    val title = text("prfnm")
    if (id == null || title == null) return null
    return FestivalEvent(
        contentId = id,
        title = title,
        address = text("fcltynm"),
        eventStartDate = normalizeDate(text("prfpdfrom")),
        eventEndDate = normalizeDate(text("prfpdto")),
        thumbnailImageUrl = text("poster"),
        imageUrl = text("poster"),
        mapX = null,
        mapY = null,
        phone = null,
        detailUrl = null,
        category = text("genrenm"),
        synopsis = null,
        source = sourceName,
    )
}

private fun normalizeDate(raw: String?): String? {
    val value = raw?.trim().orEmpty()
    if (value.isBlank()) return null
    val yyyymmdd = value.replace(".", "").replace("-", "")
    return if (yyyymmdd.length == COMPACT_DATE_LENGTH && yyyymmdd.all { it.isDigit() }) yyyymmdd else null
}

private fun Element.text(tagName: String): String? {
    val nodes = this.getElementsByTagName(tagName)
    if (nodes.length == 0) return null
    return nodes
        .item(0)
        ?.textContent
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
}

private fun Element.firstText(tagName: String): String? = text(tagName)

private fun Element.texts(tagName: String): List<String> {
    val nodes = this.getElementsByTagName(tagName)
    if (nodes.length == 0) return emptyList()
    return (0 until nodes.length)
        .mapNotNull { idx ->
            nodes
                .item(idx)
                ?.textContent
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        }.distinct()
}
