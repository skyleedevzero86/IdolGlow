package com.sleekydz86.idolglow.visit.domain

import java.util.UUID

object VisitorId {
    const val COOKIE_NAME: String = "ig_vid"
    const val LENGTH: Int = 36

    private val FORMAT = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    fun normalizeOrNull(raw: String?): String? {
        if (raw == null || !FORMAT.matches(raw)) return null
        return raw.lowercase()
    }

    fun newId(): String = UUID.randomUUID().toString()
}
