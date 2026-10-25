package com.sleekydz86.idolglow.groundedsearch.adapter.web

import java.time.LocalDate
import java.time.LocalTime

data class GroundedSlotResponse(
    val date: LocalDate,
    val start: LocalTime,
    val end: LocalTime,
)
