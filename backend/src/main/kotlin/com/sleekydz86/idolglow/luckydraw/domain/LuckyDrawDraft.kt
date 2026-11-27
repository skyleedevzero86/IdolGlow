package com.sleekydz86.idolglow.luckydraw.domain

import java.time.LocalDateTime

data class LuckyDrawDraft(
    val title: String,
    val prizeName: String,
    val prizeDescription: String,
    val winnerCount: Int,
    val entryStartsAt: LocalDateTime,
    val entryEndsAt: LocalDateTime,
    val drawAt: LocalDateTime,
) {
    fun validate() {
        require(winnerCount in MIN_WINNERS..MAX_WINNERS) { "당첨자 수는 1 이상 10 이하여야 합니다." }
        require(entryStartsAt.isBefore(entryEndsAt)) { "응모 종료 시각은 시작 시각보다 뒤여야 합니다." }
        require(!drawAt.isBefore(entryEndsAt)) { "추첨 시각은 응모 종료 시각과 같거나 뒤여야 합니다." }
        require(lengthIn(title, MAX_TITLE) && lengthIn(prizeName, MAX_TITLE)) {
            "제목과 경품 이름은 1자 이상 100자 이하여야 합니다."
        }
        require(lengthIn(prizeDescription, MAX_DESCRIPTION)) { "경품 설명은 1자 이상 1000자 이하여야 합니다." }
    }

    private fun lengthIn(
        text: String,
        max: Int,
    ): Boolean {
        val trimmed = text.trim()
        return trimmed.codePointCount(0, trimmed.length) in 1..max
    }

    companion object {
        const val MIN_WINNERS = 1
        const val MAX_WINNERS = 10
        const val MAX_TITLE = 100
        const val MAX_DESCRIPTION = 1000
    }
}
