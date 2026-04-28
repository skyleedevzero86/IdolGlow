package com.sleekydz86.idolglow.global.graphql

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

fun Long.asGraphQlId(): String = toString()
fun BigDecimal.asGraphQlNumber(): String = stripTrailingZeros().toPlainString()
fun LocalDate?.asGraphQlValue(): String? = this?.toString()
fun LocalTime?.asGraphQlValue(): String? = this?.toString()
fun LocalDateTime?.asGraphQlValue(): String? = this?.toString()
