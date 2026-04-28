package com.sleekydz86.idolglow.eventinfo.domain

data class SpecialDayInfo(
    val dateName: String,
    val locDate: String,
    val dateKind: String?,
    val isHoliday: String?,
    val seq: Int?,
    val source: String,
)
