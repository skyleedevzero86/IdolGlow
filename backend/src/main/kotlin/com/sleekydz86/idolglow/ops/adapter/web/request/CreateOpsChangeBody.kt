package com.sleekydz86.idolglow.ops.adapter.web.request

data class CreateOpsChangeBody(
    val type: String,
    val target: String,
    val payload: String,
    val requestedByKind: String? = null,
    val requestedBy: String? = null,
)
