package com.sleekydz86.idolglow.eventinfo.domain

object EventLocale {
    const val KOREAN: String = "ko"
    const val ENGLISH: String = "en"

    fun requested(
        lang: String?,
        acceptLanguage: String?,
    ): String {
        val query = lang?.trim()?.takeIf { it.isNotEmpty() }
        val header = acceptLanguage?.substringBefore(',')?.substringBefore(';')?.trim()?.takeIf { it.isNotEmpty() }
        return normalize(query ?: header)
    }

    fun normalize(raw: String?): String {
        val token =
            raw
                ?.trim()
                ?.lowercase()
                ?.replace('_', '-')
                ?.substringBefore('-')
                ?.substringBefore(',')
                ?.filter { it.isLetter() }
                ?.take(MAX_LENGTH)
                .orEmpty()
        return when (token) {
            "jp" -> "ja"
            "" -> KOREAN
            else -> token
        }
    }

    fun order(requested: String): List<String> = listOf(normalize(requested), ENGLISH, KOREAN).distinct()

    private const val MAX_LENGTH: Int = 8
}
