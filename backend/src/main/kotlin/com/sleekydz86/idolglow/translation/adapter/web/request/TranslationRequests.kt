package com.sleekydz86.idolglow.translation.adapter.web.request

data class TranslationLookupRequest(
    val items: List<TranslationLookupItem> = emptyList(),
)

data class TranslationLookupItem(
    val type: String = "",
    val key: String = "",
)

data class TranslationEditRequest(
    val fields: Map<String, String> = emptyMap(),
)

data class GlossaryTermRequest(
    val sourceTerm: String = "",
    val mode: String = "",
    val locale: String? = null,
    val targetTerm: String? = null,
)
