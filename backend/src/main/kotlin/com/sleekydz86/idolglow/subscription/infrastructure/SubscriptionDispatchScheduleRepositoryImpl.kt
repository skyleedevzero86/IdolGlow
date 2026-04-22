package com.sleekydz86.idolglow.subscription.infrastructure

import com.sleekydz86.idolglow.subscription.application.port.out.SubscriptionDispatchSchedulePort
import com.sleekydz86.idolglow.subscription.domain.SubscriptionContentType
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchSchedule
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

@Repository
class SubscriptionDispatchScheduleRepositoryImpl(
    private val subscriptionDispatchScheduleJpaRepository: SubscriptionDispatchScheduleJpaRepository,
) : SubscriptionDispatchSchedulePort {

    override fun findAllByLatest(): List<SubscriptionDispatchSchedule> =
        subscriptionDispatchScheduleJpaRepository.findAll(
            Sort.by(
                Sort.Order.asc("contentType"),
                Sort.Order.desc("createdAt"),
            )
        )

    override fun findAllActive(): List<SubscriptionDispatchSchedule> =
        subscriptionDispatchScheduleJpaRepository.findAllByActiveTrueOrderByCreatedAtDesc()

    override fun findByContentType(contentType: SubscriptionContentType): SubscriptionDispatchSchedule? =
        subscriptionDispatchScheduleJpaRepository.findByContentType(contentType)

    override fun save(schedule: SubscriptionDispatchSchedule): SubscriptionDispatchSchedule =
        subscriptionDispatchScheduleJpaRepository.save(schedule)
}
