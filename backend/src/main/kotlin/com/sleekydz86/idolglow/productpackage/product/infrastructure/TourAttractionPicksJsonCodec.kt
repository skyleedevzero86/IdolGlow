package com.sleekydz86.idolglow.productpackage.product.infrastructure

import com.sleekydz86.idolglow.productpackage.product.domain.dto.TourAttractionPickPayload
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper

object TourAttractionPicksJsonCodec {
    private val listType = object : TypeReference<List<TourAttractionPickPayload>>() {}

    fun decode(json: String?, objectMapper: ObjectMapper): List<TourAttractionPickPayload> {
        if (json.isNullOrBlank()) {
            return emptyList()
        }
        return try {
            dedupePreservingOrder(objectMapper.readValue(json, listType))
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun encode(picks: List<TourAttractionPickPayload>, objectMapper: ObjectMapper): String? {
        val deduped = dedupePreservingOrder(picks)
        if (deduped.isEmpty()) {
            return null
        }
        return objectMapper.writeValueAsString(deduped)
    }

    private fun dedupePreservingOrder(picks: List<TourAttractionPickPayload>): List<TourAttractionPickPayload> {
        val seen = LinkedHashSet<String>()
        val out = ArrayList<TourAttractionPickPayload>(picks.size)
        for (pick in picks) {
            val key = pick.attractionCode.trim().ifEmpty {
                listOf(
                    pick.name.trim(),
                    pick.mapX?.toString().orEmpty(),
                    pick.mapY?.toString().orEmpty(),
                ).joinToString("|")
            }
            if (key.isBlank()) {
                continue
            }
            if (seen.add(key)) {
                out.add(pick)
            }
        }
        return out
    }
}
