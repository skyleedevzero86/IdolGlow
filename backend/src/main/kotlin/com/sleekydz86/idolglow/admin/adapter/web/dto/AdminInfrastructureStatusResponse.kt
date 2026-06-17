package com.sleekydz86.idolglow.admin.ui.dto

data class AdminInfrastructureStatusResponse(
    val type: String,
    val label: String,
    val status: String,
    val message: String,
    val responseTimeMs: Long?,
    val details: Map<String, String>,
)
