package com.sleekydz86.idolglow.webzine.application.dto

import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal val issueDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.")

internal fun LocalDate.asIssueDisplayValue(): String = format(issueDateFormatter)
