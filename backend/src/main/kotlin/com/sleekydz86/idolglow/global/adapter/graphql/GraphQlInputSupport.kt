package com.sleekydz86.idolglow.global.graphql

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

fun String.toGraphQlIdLong(fieldName: String): Long =
    toLongOrNull() ?: throw IllegalArgumentException("$fieldName 값은 숫자여야 합니다.")

fun String.toGraphQlBigDecimal(fieldName: String): BigDecimal =
    toBigDecimalOrNull() ?: throw IllegalArgumentException("$fieldName 값은 숫자 형식이어야 합니다.")

fun String.toGraphQlLocalDate(fieldName: String): LocalDate =
    runCatching { LocalDate.parse(this) }
        .getOrElse { throw IllegalArgumentException("$fieldName 값은 yyyy-MM-dd 형식이어야 합니다.") }

fun String.toGraphQlLocalDateTime(fieldName: String): LocalDateTime =
    runCatching { LocalDateTime.parse(this) }
        .getOrElse { throw IllegalArgumentException("$fieldName 값은 yyyy-MM-ddTHH:mm:ss 형식이어야 합니다.") }
