package com.sleekydz86.idolglow.eventinfo.domain

data class CollectedBatch(
    val items: List<CollectedPerformance>,
    val incompleteReason: String? = null,
)
