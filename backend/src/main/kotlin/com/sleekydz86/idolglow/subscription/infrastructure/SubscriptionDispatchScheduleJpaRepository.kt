package com.sleekydz86.idolglow.subscription.infrastructure

import com.sleekydz86.idolglow.subscription.domain.SubscriptionContentType
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchSchedule
import org.springframework.data.jpa.repository.JpaRepository

interface SubscriptionDispatchScheduleJpaRepository : JpaRepository<SubscriptionDispatchSchedule, Long> {
    fun findByContentType(contentType: SubscriptionContentType): SubscriptionDispatchSchedule?
    fun findAllByActiveTrueOrderByCreatedAtDesc(): List<SubscriptionDispatchSchedule>
}
