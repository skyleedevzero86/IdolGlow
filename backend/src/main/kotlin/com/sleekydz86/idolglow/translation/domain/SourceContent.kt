package com.sleekydz86.idolglow.translation.domain

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

data class SourceContent(
    val type: TranslationContentType,
    val key: String,
    val fields: Map<String, String>,
    val publicVisible: Boolean,
) {
    val length: Int = fields.values.sumOf { it.length }

    fun hash(): String {
        val canonical =
            buildString {
                append(type.name).append('|').append(key)
                fields.toSortedMap().forEach { (name, value) -> append('|').append(name).append('=').append(value) }
            }
        val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    companion object {
        fun of(
            type: TranslationContentType,
            key: String,
            raw: Map<String, String?>,
            publicVisible: Boolean,
        ): SourceContent =
            SourceContent(
                type = type,
                key = key,
                fields =
                    type.fields
                        .mapNotNull { name -> raw[name]?.trim()?.takeIf { it.isNotEmpty() }?.let { name to it } }
                        .toMap(),
                publicVisible = publicVisible,
            )
    }
}

data class SourcePage(
    val items: List<SourceContent>,
    val scanned: Int,
)
