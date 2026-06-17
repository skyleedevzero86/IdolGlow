package com.sleekydz86.idolglow.admin.ui.dto

data class AdminDiskStatusResponse(
    val mountPath: String,
    val fileStoreName: String,
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val usagePercent: Double,
)
