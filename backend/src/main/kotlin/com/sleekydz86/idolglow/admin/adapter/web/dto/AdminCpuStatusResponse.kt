package com.sleekydz86.idolglow.admin.ui.dto

data class AdminCpuStatusResponse(
    val systemUsagePercent: Double?,
    val processUsagePercent: Double?,
    val systemLoadAverage: Double?,
    val availableProcessors: Int,
)
