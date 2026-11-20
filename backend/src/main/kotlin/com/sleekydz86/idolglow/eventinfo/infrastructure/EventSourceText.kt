package com.sleekydz86.idolglow.eventinfo.infrastructure

import org.springframework.web.util.HtmlUtils

object EventSourceText {
    private const val NO_BREAK_SPACE: Char = ' '
    private val SPACES = Regex(" {2,}")

    fun decode(raw: String): String {
        if (!raw.contains('&')) return raw.trim()
        return HtmlUtils
            .htmlUnescape(raw)
            .replace(NO_BREAK_SPACE, ' ')
            .replace(SPACES, " ")
            .trim()
    }

    fun decodeOrNull(raw: String?): String? = raw?.let(::decode)?.takeIf { it.isNotEmpty() }
}
