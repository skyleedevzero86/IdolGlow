package com.sleekydz86.idolglow.partner.domain

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.Base64

data class PartnerFeedCursor(
    val updatedAt: LocalDateTime,
    val globalId: String,
) {
    fun encode(): String {
        val text = "$updatedAt$SEPARATOR$globalId"
        return Base64.getUrlEncoder().withoutPadding().encodeToString(text.toByteArray(Charsets.UTF_8))
    }

    companion object {
        private const val SEPARATOR: Char = '|'
        val BEGINNING: LocalDateTime = LocalDateTime.of(2000, 1, 1, 0, 0)

        fun decode(raw: String): PartnerFeedCursor {
            val text =
                runCatching { String(Base64.getUrlDecoder().decode(raw.trim()), Charsets.UTF_8) }.getOrElse {
                    throw IllegalArgumentException("커서 형식이 올바르지 않습니다.")
                }
            val parts = text.split(SEPARATOR, limit = 2)
            require(parts.size == 2 && parts[1].isNotBlank()) { "커서 형식이 올바르지 않습니다." }
            return PartnerFeedCursor(parseLocal(parts[0]), parts[1])
        }

        fun since(raw: String?): PartnerFeedCursor {
            val text = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return PartnerFeedCursor(BEGINNING, "")
            val updatedAt =
                runCatching { OffsetDateTime.parse(text).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime() }
                    .getOrElse { parseLocal(text) }
            return PartnerFeedCursor(updatedAt, "")
        }

        private fun parseLocal(text: String): LocalDateTime =
            try {
                LocalDateTime.parse(text)
            } catch (e: DateTimeParseException) {
                throw IllegalArgumentException("시각 형식이 올바르지 않습니다. 예: 2026-11-21T09:00:00+09:00", e)
            }
    }
}
