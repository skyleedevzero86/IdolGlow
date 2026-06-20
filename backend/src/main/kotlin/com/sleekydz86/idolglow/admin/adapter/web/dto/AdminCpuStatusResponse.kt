package com.sleekydz86.idolglow.admin.adapter.web.dto

data class AdminCpuStatusResponse(
    val systemUsagePercent: Double?,
    val processUsagePercent: Double?,
    val systemLoadAverage: Double?,
    val availableProcessors: Int,
)
