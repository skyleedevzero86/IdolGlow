package com.sleekydz86.idolglow.admin.adapter.web.dto

data class AdminMemoryStatusResponse(
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val maxBytes: Long,
    val usagePercent: Double,
)
