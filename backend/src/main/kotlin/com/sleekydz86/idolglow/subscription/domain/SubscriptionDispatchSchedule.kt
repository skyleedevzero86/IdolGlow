package com.sleekydz86.idolglow.subscription.domain

import com.sleekydz86.idolglow.global.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.DayOfWeek
import java.time.LocalDateTime

@Entity
@Table(
    name = "subscription_dispatch_schedules",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_subscription_dispatch_schedule_content_type", columnNames = ["content_type"]),
    ]
)
class SubscriptionDispatchSchedule(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 40)
    val contentType: SubscriptionContentType,

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency_type", nullable = false, length = 20)
    var frequencyType: SubscriptionDispatchFrequency,

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", length = 20)
    var dayOfWeek: DayOfWeek? = null,

    @Column(name = "dispatch_hour", nullable = false)
    var dispatchHour: Int,

    @Column(name = "dispatch_minute", nullable = false)
    var dispatchMinute: Int,

    @Column(nullable = false)
    var active: Boolean,
) : BaseEntity() {

    fun update(
        frequencyType: SubscriptionDispatchFrequency,
        dayOfWeek: DayOfWeek?,
        dispatchHour: Int,
        dispatchMinute: Int,
        active: Boolean,
    ) {
        validate(frequencyType, dayOfWeek, dispatchHour, dispatchMinute)
        this.frequencyType = frequencyType
        this.dayOfWeek = dayOfWeek
        this.dispatchHour = dispatchHour
        this.dispatchMinute = dispatchMinute
        this.active = active
    }

    fun shouldTriggerAt(now: LocalDateTime): Boolean {
        if (!active) return false
        if (dispatchHour != now.hour || dispatchMinute != now.minute) return false

        return when (frequencyType) {
            SubscriptionDispatchFrequency.DAILY -> true
            SubscriptionDispatchFrequency.WEEKLY -> dayOfWeek == now.dayOfWeek
        }
    }

    companion object {
        fun create(
            contentType: SubscriptionContentType,
            frequencyType: SubscriptionDispatchFrequency,
            dayOfWeek: DayOfWeek?,
            dispatchHour: Int,
            dispatchMinute: Int,
            active: Boolean,
        ): SubscriptionDispatchSchedule {
            validate(frequencyType, dayOfWeek, dispatchHour, dispatchMinute)

            return SubscriptionDispatchSchedule(
                contentType = contentType,
                frequencyType = frequencyType,
                dayOfWeek = dayOfWeek,
                dispatchHour = dispatchHour,
                dispatchMinute = dispatchMinute,
                active = active,
            )
        }

        private fun validate(
            frequencyType: SubscriptionDispatchFrequency,
            dayOfWeek: DayOfWeek?,
            dispatchHour: Int,
            dispatchMinute: Int,
        ) {
            require(dispatchHour in 0..23) { "발송 시간(hour)은 0~23 범위여야 합니다." }
            require(dispatchMinute in 0..59) { "발송 시간(minute)은 0~59 범위여야 합니다." }
            require(frequencyType != SubscriptionDispatchFrequency.WEEKLY || dayOfWeek != null) {
                "주간 발송은 요일 선택이 필요합니다."
            }
        }
    }
}

enum class SubscriptionDispatchFrequency(val label: String) {
    DAILY("매일"),
    WEEKLY("매주"),
}
