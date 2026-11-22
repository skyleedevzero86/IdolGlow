package com.sleekydz86.idolglow.idolglowissue.application.dto

data class UpsertIdolGlowArticleSectionCommand(
    val heading: String? = null,
    val body: String,
    val note: String? = null,
)
