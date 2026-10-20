package com.sleekydz86.idolglow.global.adapter.graphql

import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

fun Long.asGraphQlId(): String = toString()

fun BigDecimal.asGraphQlNumber(): String = stripTrailingZeros().toPlainString()

fun LocalDate?.asGraphQlValue(): String? = IdolGlowDateTimes.toDisplay(this)

fun LocalTime?.asGraphQlValue(): String? = IdolGlowDateTimes.toDisplay(this)

fun LocalDateTime?.asGraphQlValue(): String? = IdolGlowDateTimes.toDisplay(this)
