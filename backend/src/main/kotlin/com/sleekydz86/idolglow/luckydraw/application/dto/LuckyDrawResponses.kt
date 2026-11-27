package com.sleekydz86.idolglow.luckydraw.application.dto

import com.sleekydz86.idolglow.luckydraw.domain.LuckyDraw
import com.sleekydz86.idolglow.luckydraw.domain.LuckyDrawStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "공개 응모 행사 정보")
data class LuckyDrawSummaryResponse(
    val id: Long,
    val title: String,
    val prizeName: String,
    val prizeDescription: String,
    val winnerCount: Int,
    @field:Schema(description = "응모 시작 시각 Asia/Seoul, 포함")
    val entryStartsAt: LocalDateTime,
    @field:Schema(description = "응모 종료 시각 Asia/Seoul, 포함하지 않음")
    val entryEndsAt: LocalDateTime,
    val drawAt: LocalDateTime,
    val status: LuckyDrawStatus,
    val drawnAt: LocalDateTime?,
    @field:Schema(description = "당첨자 닉네임 일부를 가린 목록. DRAWN 일 때만", example = "[\"idol***\"]")
    val winners: List<String>,
) {
    companion object {
        fun of(
            draw: LuckyDraw,
            now: LocalDateTime,
            maskedWinners: List<String>,
        ): LuckyDrawSummaryResponse =
            LuckyDrawSummaryResponse(
                id = draw.id,
                title = draw.title,
                prizeName = draw.prizeName,
                prizeDescription = draw.prizeDescription,
                winnerCount = draw.winnerCount,
                entryStartsAt = draw.entryStartsAt,
                entryEndsAt = draw.entryEndsAt,
                drawAt = draw.drawAt,
                status = draw.status(now),
                drawnAt = draw.drawnAt,
                winners = maskedWinners,
            )
    }
}

@Schema(description = "내 응모 현황")
data class LuckyDrawMyStatusResponse(
    val drawId: Long,
    val enteredToday: Boolean,
    val entryCount: Int,
    val purchaseCount: Int,
    @field:Schema(description = "응모권. 응모 수 + 가산 결제 수 × 10, 응모 0회면 0")
    val tickets: Long,
)

@Schema(description = "관리자 응모 행사 정보")
data class LuckyDrawAdminResponse(
    val id: Long,
    val title: String,
    val prizeName: String,
    val prizeDescription: String,
    val winnerCount: Int,
    val entryStartsAt: LocalDateTime,
    val entryEndsAt: LocalDateTime,
    val drawAt: LocalDateTime,
    val status: LuckyDrawStatus,
    val drawnAt: LocalDateTime?,
    val drawSeed: Long?,
    val entrantCount: Int,
    val entryCount: Long,
    val purchaseCount: Int,
    val winners: List<LuckyDrawAdminWinner>,
)

data class LuckyDrawAdminWinner(
    val userId: Long,
    val nickname: String,
    val winnerOrder: Int,
)
