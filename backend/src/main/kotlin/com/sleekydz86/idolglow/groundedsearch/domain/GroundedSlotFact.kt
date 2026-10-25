package com.sleekydz86.idolglow.groundedsearch.domain

import java.time.LocalDate
import java.time.LocalTime

data class GroundedSlotFact(
    val date: LocalDate,
    val start: LocalTime,
    val end: LocalTime,
)
