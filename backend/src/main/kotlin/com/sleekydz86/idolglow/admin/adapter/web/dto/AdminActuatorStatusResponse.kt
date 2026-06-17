package com.sleekydz86.idolglow.admin.ui.dto

data class AdminActuatorStatusResponse(
    val enabled: Boolean,
    val healthEndpoint: String,
    val metricsEndpoint: String,
    val metrics: List<AdminActuatorMetricResponse>,
)
