package com.sleekydz86.idolglow.webzine.application.dto

data class CreateWebzineIssueCommand(
    val volume: Int,
    val issueDate: String,
    val coverImageUrl: String,
    val teaser: String,
)
