package com.sleekydz86.idolglow.subscription.application.port.out

import com.sleekydz86.idolglow.subscription.domain.SubscriptionContentType
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchSchedule

interface SubscriptionDispatchSchedulePort {
    fun findAllByLatest(): List<SubscriptionDispatchSchedule>
    fun findAllActive(): List<SubscriptionDispatchSchedule>
    fun findByContentType(contentType: SubscriptionContentType): SubscriptionDispatchSchedule?
    fun save(schedule: SubscriptionDispatchSchedule): SubscriptionDispatchSchedule
}
