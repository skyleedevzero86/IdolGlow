package com.sleekydz86.idolglow.newsletter.application.dto

import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import java.time.LocalDate

internal fun LocalDate.asNewsletterDisplayValue(): String = requireNotNull(IdolGlowDateTimes.toDisplay(this))
