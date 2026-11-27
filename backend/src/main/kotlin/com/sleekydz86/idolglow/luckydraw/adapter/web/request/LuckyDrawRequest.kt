package com.sleekydz86.idolglow.luckydraw.adapter.web.request

import com.sleekydz86.idolglow.luckydraw.domain.LuckyDrawDraft
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "응모 행사 등록·수정 요청. 시각은 Asia/Seoul")
data class LuckyDrawRequest(
    @field:Schema(description = "행사 제목 1~100자", example = "SBS 가요대전 티켓 응모")
    val title: String,
    @field:Schema(description = "경품 이름 1~100자", example = "가요대전 2층 지정석 2매")
    val prizeName: String,
    @field:Schema(description = "경품 설명 1~1000자")
    val prizeDescription: String,
    @field:Schema(description = "당첨자 수 1~10", example = "1")
    val winnerCount: Int,
    @field:Schema(description = "응모 시작 시각, 포함", example = "2026-12-01T11:00:00")
    val entryStartsAt: LocalDateTime,
    @field:Schema(description = "응모 종료 시각, 포함하지 않음", example = "2026-12-08T11:00:00")
    val entryEndsAt: LocalDateTime,
    @field:Schema(description = "추첨 시각, 종료와 같거나 뒤", example = "2026-12-08T13:00:00")
    val drawAt: LocalDateTime,
) {
    fun toDraft(): LuckyDrawDraft =
        LuckyDrawDraft(
            title = title,
            prizeName = prizeName,
            prizeDescription = prizeDescription,
            winnerCount = winnerCount,
            entryStartsAt = entryStartsAt,
            entryEndsAt = entryEndsAt,
            drawAt = drawAt,
        )
}
