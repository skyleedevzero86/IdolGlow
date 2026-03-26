package com.sleekydz86.idolglow.wish.application.event

import com.sleekydz86.idolglow.wish.domain.vo.WishAggregateType

data class WishDeleteEvent(
    val aggregateType: WishAggregateType,
    val aggregateId: Long,
)