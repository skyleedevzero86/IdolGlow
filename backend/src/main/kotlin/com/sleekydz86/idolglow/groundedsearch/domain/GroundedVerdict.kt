package com.sleekydz86.idolglow.groundedsearch.domain

data class GroundedVerdict(
    val accepted: Boolean,
    val text: String?,
    val rejectReason: String?,
)
