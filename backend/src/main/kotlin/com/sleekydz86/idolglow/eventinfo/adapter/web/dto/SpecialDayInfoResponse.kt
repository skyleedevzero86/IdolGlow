package com.sleekydz86.idolglow.eventinfo.adapter.web.dto

data class SpecialDayInfoResponse(
    val dateName: String,
    val locDate: String,
    val dateKind: String?,
    val isHoliday: String?,
    val seq: Int?,
    val source: String,
)
