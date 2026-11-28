package com.sleekydz86.idolglow.survey.domain.autopick

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "survey_auto_pick_candidates",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_survey_auto_pick_candidates_user_slot",
            columnNames = ["user_id", "reservation_slot_id"],
        ),
    ],
)
class SurveyAutoPickCandidate(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(name = "product_id", nullable = false)
    val productId: Long,
    @Column(name = "reservation_slot_id", nullable = false)
    val reservationSlotId: Long,
    @Column(name = "member_open_at", nullable = false)
    val memberOpenAt: LocalDateTime,
    @Column(name = "scheduled_charge_at", nullable = false)
    val scheduledChargeAt: LocalDateTime,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: SurveyAutoPickStatus = SurveyAutoPickStatus.SCHEDULED,
    @Enumerated(EnumType.STRING)
    @Column(name = "fail_reason", length = 30)
    var failReason: SurveyAutoPickFailReason? = null,
    @Column(name = "reservation_id")
    var reservationId: Long? = null,
) : BaseEntity() {
    fun canSkip(now: LocalDateTime): Boolean =
        status == SurveyAutoPickStatus.SCHEDULED && now.isBefore(scheduledChargeAt)

    fun isDue(now: LocalDateTime): Boolean =
        status == SurveyAutoPickStatus.SCHEDULED && !scheduledChargeAt.isAfter(now)

    fun skip(now: LocalDateTime) {
        check(canSkip(now)) { "결제 예정 시각 전의 예정 후보만 건너뛸 수 있습니다. status=$status" }
        status = SurveyAutoPickStatus.SKIPPED
    }

    fun startProcessing() {
        check(status == SurveyAutoPickStatus.SCHEDULED) { "예정 후보만 결제를 시작할 수 있습니다. status=$status" }
        status = SurveyAutoPickStatus.PROCESSING
    }

    fun book(reservationId: Long) {
        check(status == SurveyAutoPickStatus.PROCESSING) {
            "결제 중인 후보만 예약 완료로 바꿀 수 있습니다. status=$status"
        }
        this.reservationId = reservationId
        status = SurveyAutoPickStatus.BOOKED
    }

    fun fail(reason: SurveyAutoPickFailReason) {
        check(status == SurveyAutoPickStatus.SCHEDULED || status == SurveyAutoPickStatus.PROCESSING) {
            "끝난 후보는 실패로 바꿀 수 없습니다. status=$status"
        }
        failReason = reason
        status = SurveyAutoPickStatus.FAILED
    }

    companion object {
        const val CHARGE_DELAY_MINUTES = 10L
    }
}
