package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.domain.FestivalCommonDetail
import com.sleekydz86.idolglow.eventinfo.domain.FestivalEvent
import com.sleekydz86.idolglow.global.infrastructure.config.CultureInfoApiProperties
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
class CultureInfoApiClient(
    private val webClient: WebClient,
    private val properties: CultureInfoApiProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun listEventsForCalendarDay(
        yyyyMMdd: String,
        pageNo: Int,
        numOfRows: Int,
        maxRealm2PerServiceTp: Int = 1000,
        maxArea2PerServiceTp: Int = 1000,
    ): List<FestivalEvent> {
        val key = resolveEncodedServiceKey(properties.serviceKey)
        if (key.isEmpty()) return emptyList()
        val perTp = (numOfRows / SERVICE_TYPES.size).coerceAtLeast(1).coerceAtMost(1000)
        val dateParam = properties.livelihoodDateParam.trim().ifBlank { "stdDate" }
        val base = SERVICE_TYPES.flatMap { tp ->
            val livelihood = fetchPeriodLikeItems("livelihood2", key, pageNo, perTp) {
                mapOf(
                    "serviceTp" to tp,
                    dateParam to yyyyMMdd,
                )
            }.mapNotNull { mapCultureItem(it, tp, "CULTURE_CALENDAR") }
            if (livelihood.isNotEmpty()) {
                livelihood
            } else {
                fetchPeriodLikeItems("period2", key, pageNo, perTp) {
                    mapOf(
                        "serviceTp" to tp,
                        "from" to yyyyMMdd,
                        "to" to yyyyMMdd,
                    )
                }.mapNotNull { mapCultureItem(it, tp, "CULTURE_CALENDAR") }
            }
        }
        val realm2 = listRealm2ForSingleDay(yyyyMMdd, key, maxPerServiceTp = maxRealm2PerServiceTp.coerceIn(1, 1000))
        val area2 = listArea2ForSingleDay(yyyyMMdd, key, maxPerServiceTp = maxArea2PerServiceTp.coerceIn(1, 1000))
        return (base + realm2 + area2).distinctBy { it.contentId }
    }

    fun listArea2Events(
        fromYyyyMMdd: String,
        toYyyyMMdd: String,
        serviceTp: String,
        sortStdr: Int,
        pageNo: Int,
        numOfrows: Int,
    ): List<FestivalEvent> {
        val key = resolveEncodedServiceKey(properties.serviceKey)
        if (key.isEmpty()) return emptyList()
        val params = mapOf(
            "from" to fromYyyyMMdd,
            "to" to toYyyyMMdd,
            "serviceTp" to serviceTp,
            "sortStdr" to sortStdr.coerceIn(1, 3).toString(),
            "PageNo" to pageNo.coerceAtLeast(1).toString(),
            "numOfrows" to numOfrows.coerceIn(1, 1000).toString(),
        )
        val xml = fetchXml(buildCultureUrl("area2", key, params)) ?: return emptyList()
        if (!isResultOk(xml)) {
            log.warn("문화시설정보 area2 비정상 응답. serviceTp={}, bodyPrefix={}", serviceTp, xml.take(200))
            return emptyList()
        }
        return parseItems(xml).mapNotNull { mapCultureItem(it, serviceTp, "CULTURE_AREA2") }
    }

    private fun listArea2ForSingleDay(yyyyMMdd: String, encodedKey: String, maxPerServiceTp: Int): List<FestivalEvent> {
        val rows = maxPerServiceTp.coerceIn(1, 1000)
        return SERVICE_TYPES.flatMap { tp ->
            val params = mapOf(
                "from" to yyyyMMdd,
                "to" to yyyyMMdd,
                "serviceTp" to tp,
                "sortStdr" to "1",
                "PageNo" to "1",
                "numOfrows" to rows.toString(),
            )
            val xml = fetchXml(buildCultureUrl("area2", encodedKey, params)) ?: return@flatMap emptyList()
            if (!isResultOk(xml)) {
                log.debug("문화시설정보 area2 건너뜀. tp={}", tp)
                return@flatMap emptyList()
            }
            parseItems(xml).mapNotNull { mapCultureItem(it, tp, "CULTURE_AREA2") }
        }
    }

    fun listRealm2Events(
        fromYyyyMMdd: String,
        toYyyyMMdd: String,
        serviceTp: String,
        realmCode: String?,
        sortStdr: Int,
        pageNo: Int,
        numOfrows: Int,
    ): List<FestivalEvent> {
        val key = resolveEncodedServiceKey(properties.serviceKey)
        if (key.isEmpty()) return emptyList()
        val params = buildMap {
            put("from", fromYyyyMMdd)
            put("to", toYyyyMMdd)
            put("serviceTp", serviceTp)
            put("sortStdr", sortStdr.coerceIn(1, 3).toString())
            put("PageNo", pageNo.coerceAtLeast(1).toString())
            put("numOfrows", numOfrows.coerceIn(1, 1000).toString())
            if (!realmCode.isNullOrBlank()) put("realmCode", realmCode.trim())
        }
        val xml = fetchXml(buildCultureUrl("realm2", key, params)) ?: return emptyList()
        if (!isResultOk(xml)) {
            log.warn("문화시설정보 realm2 비정상 응답. serviceTp={}, bodyPrefix={}", serviceTp, xml.take(200))
            return emptyList()
        }
        return parseItems(xml).mapNotNull { mapCultureItem(it, serviceTp, "CULTURE_REALM2") }
    }

    private fun listRealm2ForSingleDay(yyyyMMdd: String, encodedKey: String, maxPerServiceTp: Int): List<FestivalEvent> {
        val perRealmRows = (maxPerServiceTp / REALM_CODES.size).coerceAtLeast(1).coerceAtMost(1000)
        return SERVICE_TYPES.flatMap { tp ->
            REALM_CODES.flatMap { code ->
                val params = mapOf(
                    "from" to yyyyMMdd,
                    "to" to yyyyMMdd,
                    "serviceTp" to tp,
                    "sortStdr" to "1",
                    "PageNo" to "1",
                    "numOfrows" to perRealmRows.toString(),
                    "realmCode" to code,
                )
                val xml = fetchXml(buildCultureUrl("realm2", encodedKey, params)) ?: return@flatMap emptyList()
                if (!isResultOk(xml)) {
                    log.debug("문화시설정보 realm2 건너뜀. tp={}, realmCode={}", tp, code)
                    return@flatMap emptyList()
                }
                parseItems(xml).mapNotNull { mapCultureItem(it, tp, "CULTURE_REALM2") }
            }
        }
    }

    fun detail(seq: String): FestivalCommonDetail? {
        val key = resolveEncodedServiceKey(properties.serviceKey)
        if (key.isEmpty()) return null
        val url = buildCultureUrl("detail2", key, mapOf("seq" to seq.trim()))
        val xml = fetchXml(url) ?: return null
        if (!isResultOk(xml)) {
            log.warn("문화시설정보 detail2 비정상 응답. seq={}, bodyPrefix={}", seq, xml.take(200))
            return null
        }
        val item = parseItems(xml).firstOrNull() ?: return null
        val m = item.toFieldMap()
        val title = m["title"].orEmpty().ifBlank { return null }
        val place = m["place"].orEmpty()
        val area = m["area"].orEmpty()
        val sigungu = m["sigungu"].orEmpty()
        val address = listOf(area, sigungu, place).filter { it.isNotBlank() }.joinToString(" ").ifBlank { null }
        val overview = listOf("contents", "content", "description", "overview", "subTitle", "summary")
            .mapNotNull { k -> m[k]?.takeIf { it.isNotBlank() } }
            .firstOrNull()
        val tel = m["tel"]?.ifBlank { null } ?: m["phone"]?.ifBlank { null }
        val homepage = m["homepage"]?.ifBlank { null } ?: m["url"]?.ifBlank { null } ?: m["link"]?.ifBlank { null }
        val img = m["thumbnail"]?.ifBlank { null } ?: m["posterimg"]?.ifBlank { null } ?: m["firstimage"]?.ifBlank { null }
        val mapX = m["gpsX"]?.toDoubleOrNull() ?: m["mapx"]?.toDoubleOrNull()
        val mapY = m["gpsY"]?.toDoubleOrNull() ?: m["mapy"]?.toDoubleOrNull()
        return FestivalCommonDetail(
            contentId = "culture-${m["seq"] ?: seq}",
            contentTypeId = m["serviceName"]?.ifBlank { null },
            title = title,
            homepage = homepage,
            overview = overview,
            address = address,
            addressDetail = null,
            mapX = mapX,
            mapY = mapY,
            tel = tel,
            firstImage = img,
            firstImage2 = img,
        )
    }

    private fun fetchPeriodLikeItems(
        operation: String,
        encodedKey: String,
        pageNo: Int,
        numOfrows: Int,
        extra: () -> Map<String, String>,
    ): List<Element> {
        val params = buildMap {
            putAll(extra())
            put("PageNo", pageNo.toString())
            put("numOfrows", numOfrows.coerceIn(1, 1000).toString())
        }
        val xml = fetchXml(buildCultureUrl(operation, encodedKey, params)) ?: return emptyList()
        if (!isResultOk(xml)) {
            log.warn("문화시설정보 {} 비정상 응답. bodyPrefix={}", operation, xml.take(200))
            return emptyList()
        }
        return parseItems(xml)
    }

    private fun mapCultureItem(el: Element, serviceTp: String, sourceRoot: String): FestivalEvent? {
        val m = el.toFieldMap()
        val seq = m["seq"]?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val title = m["title"]?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val place = m["place"].orEmpty()
        val area = m["area"].orEmpty()
        val sigungu = m["sigungu"].orEmpty()
        val address = listOf(area, sigungu, place).filter { it.isNotBlank() }.joinToString(" ").ifBlank { null }
        val start = m["startDate"]?.trim()?.takeIf { it.isNotEmpty() }
        val end = m["endDate"]?.trim()?.takeIf { it.isNotEmpty() }
        val thumb = m["thumbnail"]?.trim()?.takeIf { it.isNotEmpty() }
        val mapX = m["gpsX"]?.toDoubleOrNull()
        val mapY = m["gpsY"]?.toDoubleOrNull()
        val realm = m["realmName"]?.trim()?.takeIf { it.isNotEmpty() }
        val serviceName = m["serviceName"]?.trim()?.takeIf { it.isNotEmpty() }
        val category = listOfNotNull(serviceName, realm).joinToString(" · ").ifBlank { null }
        return FestivalEvent(
            contentId = "culture-$seq",
            title = title,
            address = address,
            eventStartDate = start,
            eventEndDate = end,
            thumbnailImageUrl = thumb,
            imageUrl = thumb,
            mapX = mapX,
            mapY = mapY,
            phone = null,
            detailUrl = "https://www.culture.go.kr/wday/index_wday.html",
            category = category,
            synopsis = null,
            source = "${sourceRoot}_$serviceTp",
        )
    }

    private fun fetchXml(url: String): String? =
        runCatching {
            webClient.get().uri(url).retrieve().bodyToMono(String::class.java).block()
        }.getOrElse {
            log.warn("문화시설정보 API 호출 실패: {}", it.message)
            null
        }

    private fun isResultOk(xml: String): Boolean {
        val code = RESULT_CODE_REGEX.find(xml)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        return code == "00"
    }

    private fun parseItems(xml: String): List<Element> {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false
        val doc = factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
        val nodeList = doc.getElementsByTagName("item")
        return (0 until nodeList.length).mapNotNull { nodeList.item(it) as? Element }
    }

    private fun Element.toFieldMap(): Map<String, String> {
        val map = linkedMapOf<String, String>()
        for (i in 0 until childNodes.length) {
            val n = childNodes.item(i)
            if (n is Element) {
                val text = n.textContent?.trim().orEmpty()
                if (text.isNotEmpty()) {
                    map[n.tagName] = text
                }
            }
        }
        return map
    }

    private fun buildCultureUrl(operation: String, encodedServiceKey: String, params: Map<String, String>): String {
        val query = params.entries
            .filter { it.value.isNotBlank() }
            .joinToString("&") { (k, v) ->
                "$k=${UriUtils.encodeQueryParam(v, StandardCharsets.UTF_8)}"
            }
        val base = properties.baseUrl.trimEnd('/')
        return "$base/$operation?serviceKey=$encodedServiceKey&$query"
    }

    private fun resolveEncodedServiceKey(rawServiceKey: String): String {
        val trimmed = rawServiceKey.trim().removePrefix("serviceKey=").removePrefix("CULTURE_INFO_SERVICE_KEY=")
        if (trimmed.isEmpty()) return ""
        return if (trimmed.contains('%')) trimmed else UriUtils.encodeQueryParam(trimmed, StandardCharsets.UTF_8)
    }

    companion object {
        private val SERVICE_TYPES = listOf("A", "B", "C")
        private val REALM_CODES = listOf(
            "A000",
            "B000",
            "B002",
            "C000",
            "D000",
            "B003",
            "E000",
            "F000",
            "G000",
            "H000",
            "I000",
            "L000",
        )
        private val RESULT_CODE_REGEX = Regex("<resultCode>\\s*([^<]+)\\s*</resultCode>", RegexOption.IGNORE_CASE)
    }
}
