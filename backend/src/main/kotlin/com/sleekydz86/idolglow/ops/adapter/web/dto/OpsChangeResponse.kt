package com.sleekydz86.idolglow.ops.adapter.web.dto

import com.sleekydz86.idolglow.ops.domain.OpsChangeRequest

data class OpsChangeResponse(
    val id: Long,
    val type: String,
    val target: String,
    val payload: String,
    val requestedBy: String,
    val requestedByKind: String,
    val status: String,
    val beforeSnapshot: String?,
    val approvedBy: String?,
    val appliedAt: String?,
)

fun OpsChangeRequest.toResponse(): OpsChangeResponse =
    OpsChangeResponse(
        id = id,
        type = type,
        target = target,
        payload = payload,
        requestedBy = requestedBy,
        requestedByKind = requestedByKind.name,
        status = status.name,
        beforeSnapshot = beforeSnapshot,
        approvedBy = approvedBy,
        appliedAt = appliedAt?.toString(),
    )
