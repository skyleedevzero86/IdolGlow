package com.sleekydz86.idolglow.webzine.application.dto

import com.sleekydz86.idolglow.webzine.domain.IssueCategory

data class UpsertWebzineArticleSectionCommand(
    val heading: String? = null,
    val body: String,
    val note: String? = null,
)
