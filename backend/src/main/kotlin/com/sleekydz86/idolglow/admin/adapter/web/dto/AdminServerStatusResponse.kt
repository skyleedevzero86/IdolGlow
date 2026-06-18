package com.sleekydz86.idolglow.admin.adapter.web.dto

data class AdminServerStatusResponse(
    val generatedAt: String,
    val overallStatus: String,
    val summary: AdminServerSummaryResponse,
    val system: AdminSystemStatusResponse,
    val infrastructure: List<AdminInfrastructureStatusResponse>,
    val actuator: AdminActuatorStatusResponse,
)
