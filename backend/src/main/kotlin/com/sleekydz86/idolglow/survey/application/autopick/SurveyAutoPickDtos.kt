package com.sleekydz86.idolglow.survey.application.autopick

import com.sleekydz86.idolglow.survey.domain.autopick.SurveyAutoPickCandidate
import com.sleekydz86.idolglow.survey.domain.autopick.SurveyAutoPickFailReason
import com.sleekydz86.idolglow.survey.domain.autopick.SurveyAutoPickStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDateTime

data class SurveyAutoPickCommand(
    val enabled: Boolean,
    val productIds: List<Long>,
    val maxPrice: BigDecimal,
)

@Schema(description = "자동 담기 설정과 최근 결제 예정 내역")
data class SurveyAutoPickResponse(
    val enabled: Boolean,
    val productIds: List<Long>,
    val maxPrice: BigDecimal?,
    @field:Schema(description = "한 달 자동 예약 최대 건수")
    val monthlyLimit: Int,
    @field:Schema(description = "이번 달 자동 예약 건수")
    val bookedThisMonth: Long,
    val candidates: List<SurveyAutoPickCandidateResponse>,
)

@Schema(description = "자동 담기 결제 예정 후보")
data class SurveyAutoPickCandidateResponse(
    val id: Long,
    val productId: Long,
    val reservationSlotId: Long,
    val status: SurveyAutoPickStatus,
    val memberOpenAt: LocalDateTime,
    val scheduledChargeAt: LocalDateTime,
    val failReason: SurveyAutoPickFailReason?,
    val reservationId: Long?,
) {
    companion object {
        fun from(candidate: SurveyAutoPickCandidate) =
            SurveyAutoPickCandidateResponse(
                id = candidate.id,
                productId = candidate.productId,
                reservationSlotId = candidate.reservationSlotId,
                status = candidate.status,
                memberOpenAt = candidate.memberOpenAt,
                scheduledChargeAt = candidate.scheduledChargeAt,
                failReason = candidate.failReason,
                reservationId = candidate.reservationId,
            )
    }
}
