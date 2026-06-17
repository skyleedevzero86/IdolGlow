package com.sleekydz86.idolglow.webzine.application.dto

import com.sleekydz86.idolglow.webzine.domain.WebzineArticle
import com.sleekydz86.idolglow.webzine.domain.WebzineArticleSection
import com.sleekydz86.idolglow.webzine.domain.WebzineIssue
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal val issueDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.")

internal fun LocalDate.asIssueDisplayValue(): String = format(issueDateFormatter)
