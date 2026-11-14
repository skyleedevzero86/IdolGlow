package com.sleekydz86.idolglow.idolglowissue.application.dto

data class CreateIdolGlowIssueCommand(
    val volume: Int,
    val issueDate: String,
    val coverImageUrl: String,
    val teaser: String,
)
