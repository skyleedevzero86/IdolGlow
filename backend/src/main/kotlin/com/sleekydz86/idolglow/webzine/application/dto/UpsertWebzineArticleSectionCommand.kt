package com.sleekydz86.idolglow.webzine.application.dto

data class UpsertWebzineArticleSectionCommand(
    val heading: String? = null,
    val body: String,
    val note: String? = null,
)
