package com.sleekydz86.idolglow.luckydraw.application.event

data class LuckyDrawCompletedEvent(
    val drawId: Long,
    val title: String,
    val prizeName: String,
    val winnerUserIds: List<Long>,
)
