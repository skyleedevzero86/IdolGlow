package com.sleekydz86.idolglow.eventinfo.application

data class EventMatchConflictView(
    val source: String,
    val contentId: String,
    val title: String,
    val startDate: String?,
    val endDate: String?,
    val venue: String?,
    val matchStatus: String,
    val globalId: String?,
)

data class EventMatchComparisonView(
    val left: EventMatchConflictView,
    val right: EventMatchConflictView,
    val sameNormalizedTitle: Boolean,
    val periodsOverlap: Boolean,
    val sameVenue: Boolean,
    val reason: String,
)

data class EventMatchProposalView(
    val requestId: Long,
    val status: String,
    val reason: String,
)

data class EventMatchSuggestion(
    val target: String,
    val globalId: String,
    val reason: String,
    val sameShow: Boolean,
)

data class EventMatchProposalScore(
    val total: Int,
    val correct: Int,
    val wrong: Int,
) {
    fun summary(): String = "AI 제안 ${total}건 중 ${correct}건 맞음, 틀린 ${wrong}건은 승인 단계에서 걸러짐"
}
