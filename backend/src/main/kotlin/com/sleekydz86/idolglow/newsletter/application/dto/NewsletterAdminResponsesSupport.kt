package com.sleekydz86.idolglow.newsletter.application.dto

import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal val newsletterDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

internal fun LocalDate.asNewsletterDisplayValue(): String = format(newsletterDateFormatter)
