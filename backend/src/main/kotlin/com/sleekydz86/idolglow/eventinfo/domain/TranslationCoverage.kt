package com.sleekydz86.idolglow.eventinfo.domain

data class TranslationCoverage(
    val locale: String,
    val totalActive: Long,
    val visible: Long,
    val machineOnly: Long,
    val missing: Long,
    val missingSamples: List<MissingTranslation>,
) {
    fun isConsistent(): Boolean = visible + machineOnly + missing == totalActive
}

data class MissingTranslation(
    val globalId: String,
    val title: String,
)
