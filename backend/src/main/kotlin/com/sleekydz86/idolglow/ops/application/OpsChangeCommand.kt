package com.sleekydz86.idolglow.ops.application

import com.sleekydz86.idolglow.ops.domain.OpsActorKind

data class OpsChangeCommand(
    val type: String,
    val target: String,
    val payload: String,
    val kind: OpsActorKind,
    val agentName: String?,
)
