package com.sleekydz86.idolglow.mbrd.infrastructure

import org.springframework.stereotype.Component
import java.util.Locale
import kotlin.math.floorMod
import kotlin.math.sqrt

@Component
class MbrdEditorEmbeddingEncoder {
    fun encode(content: String?): String {
        val vector = DoubleArray(DIMENSIONS)
        val normalized = normalize(content ?: "")

        if (normalized.isBlank()) {
            return toVectorLiteral(vector)
        }

        for (token in normalized.split("\\s+".toRegex())) {
            val primary = floorMod(token.hashCode(), DIMENSIONS)
            val secondary = floorMod(token.hashCode() ushr 16, DIMENSIONS)
            vector[primary] += 1.0
            vector[secondary] += 0.5
        }

        normalizeVector(vector)
        return toVectorLiteral(vector)
    }

    private fun normalize(content: String): String =
        content.lowercase(Locale.ROOT).replace("[^\\p{L}\\p{N}\\s]+".toRegex(), " ").trim()

    private fun normalizeVector(vector: DoubleArray) {
        val magnitude = sqrt(vector.sumOf { it * it })
        if (magnitude == 0.0) return
        for (i in vector.indices) {
            vector[i] = vector[i] / magnitude
        }
    }

    private fun toVectorLiteral(vector: DoubleArray): String {
        val builder = StringBuilder("[")
        for (i in vector.indices) {
            if (i > 0) builder.append(", ")
            builder.append(String.format(Locale.ROOT, "%.8f", vector[i]))
        }
        return builder.append("]").toString()
    }

    companion object {
        private const val DIMENSIONS = 8
    }
}
