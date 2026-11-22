package com.sleekydz86.idolglow.eventinfo.application.port.out

interface PerformanceScheduleSourcePort {
    val source: String

    fun guidance(sourceId: String): String
}
