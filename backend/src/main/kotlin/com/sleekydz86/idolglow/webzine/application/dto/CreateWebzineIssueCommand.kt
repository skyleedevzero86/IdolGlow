package com.sleekydz86.idolglow.webzine.application.dto

import com.sleekydz86.idolglow.webzine.domain.IssueCategory

data class CreateWebzineIssueCommand(
    val volume: Int,
    val issueDate: String,
    val coverImageUrl: String,
    val teaser: String,
)
