package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.domain.EventSearchDocument
import com.sleekydz86.idolglow.eventinfo.domain.EventSearchHit
import tools.jackson.databind.JsonNode
import java.time.LocalDate

object ElasticsearchEventSearchBodies {
    private const val ACTIVE: String = "ACTIVE"
    private const val NGRAM: String = "idolglow_ngram"
    private const val NGRAM_MIN: Int = 2
    private const val NGRAM_MAX: Int = 3
    private val TRANSLATED_LOCALES: List<String> = listOf("en", "ja", "zh")
    private val SEARCH_FIELDS: List<String> = listOf("title^3", "title.ngram^2", "titles.*^2", "venue", "venue.ngram")

    fun source(document: EventSearchDocument): Map<String, Any?> =
        mapOf(
            "globalId" to document.globalId,
            "title" to document.title,
            "titles" to document.titles,
            "venue" to document.venue,
            "startDate" to document.startDate?.toString(),
            "endDate" to document.endDate?.toString(),
            "status" to document.status,
            "mergedInto" to document.mergedInto,
            "metaVersion" to document.metaVersion,
        )

    fun hit(source: JsonNode): EventSearchHit =
        EventSearchHit(
            globalId = source.path("globalId").asString(),
            title = source.path("title").asString(),
            titles = source.path("titles").properties().associate { (locale, title) -> locale to title.asString() },
            venue = source.path("venue").textOrNull(),
            startDate = source.path("startDate").textOrNull()?.let(LocalDate::parse),
            endDate = source.path("endDate").textOrNull()?.let(LocalDate::parse),
        )

    fun search(
        query: String,
        size: Int,
    ): Map<String, Any> {
        val match = mapOf("multi_match" to mapOf("query" to query, "fields" to SEARCH_FIELDS))
        val activeOnly = mapOf("term" to mapOf("status" to ACTIVE))
        val bool = mapOf("must" to listOf(match), "filter" to listOf(activeOnly))
        return mapOf("size" to size, "query" to mapOf("bool" to bool))
    }

    fun indexDefinition(): Map<String, Any> = mapOf("settings" to settings(), "mappings" to mappings())

    private fun settings(): Map<String, Any> {
        val tokenizer =
            mapOf(
                "type" to "ngram",
                "min_gram" to NGRAM_MIN,
                "max_gram" to NGRAM_MAX,
                "token_chars" to listOf("letter", "digit"),
            )
        val analyzer = mapOf("tokenizer" to NGRAM, "filter" to listOf("lowercase"))
        val analysis = mapOf("tokenizer" to mapOf(NGRAM to tokenizer), "analyzer" to mapOf(NGRAM to analyzer))
        return mapOf("analysis" to analysis)
    }

    private fun mappings(): Map<String, Any> =
        mapOf(
            "properties" to
                mapOf(
                    "globalId" to keyword(),
                    "title" to textWithNgram(),
                    "titles" to mapOf("properties" to TRANSLATED_LOCALES.associateWith { textWithNgram() }),
                    "venue" to textWithNgram(),
                    "startDate" to mapOf("type" to "date"),
                    "endDate" to mapOf("type" to "date"),
                    "status" to keyword(),
                    "mergedInto" to keyword(),
                    "metaVersion" to mapOf("type" to "long"),
                ),
        )

    private fun keyword(): Map<String, Any> = mapOf("type" to "keyword")

    private fun textWithNgram(): Map<String, Any> =
        mapOf("type" to "text", "fields" to mapOf("ngram" to mapOf("type" to "text", "analyzer" to NGRAM)))

    private fun JsonNode.textOrNull(): String? = if (isNull || isMissingNode) null else asString()
}
