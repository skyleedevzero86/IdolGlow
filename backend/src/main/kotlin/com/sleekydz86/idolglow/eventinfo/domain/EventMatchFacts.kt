package com.sleekydz86.idolglow.eventinfo.domain

import java.time.LocalDate

data class EventMatchSide(
    val title: String,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val venue: String?,
    val contentId: String,
)

data class EventMatchFacts(
    val sameNormalizedTitle: Boolean,
    val periodsOverlap: Boolean,
    val sameVenue: Boolean,
    val prompt: String,
) {
    fun sameShow(): Boolean = sameNormalizedTitle && periodsOverlap && sameVenue

    fun localReason(): String =
        when {
            sameShow() -> "같은 공연으로 보임. 근거: 기간·장소가 같고 제목은 부제만 다릅니다."
            sameNormalizedTitle && periodsOverlap ->
                "같은 공연으로 보임. 근거: 기간이 겹치고 제목이 같습니다. 장소는 다릅니다."
            else -> "다른 공연으로 보임. 근거: 제목, 기간, 장소가 모두 같지는 않습니다."
        }
}

object EventSourceComparison {
    fun side(ref: EventSourceRef): EventMatchSide =
        EventMatchSide(ref.title, ref.startDate, ref.endDate, ref.venue, ref.sourceId)

    fun side(master: EventMaster): EventMatchSide =
        EventMatchSide(master.title, master.startDate, master.endDate, master.venue, master.globalId)

    fun of(
        left: EventMatchSide,
        right: EventMatchSide,
    ): EventMatchFacts {
        val sameTitle = EventTitleNormalizer.normalize(left.title) == EventTitleNormalizer.normalize(right.title)
        return EventMatchFacts(
            sameNormalizedTitle = sameTitle,
            periodsOverlap = periodsOverlap(left, right),
            sameVenue = venuesMatch(left.venue, right.venue),
            prompt = prompt(left, right),
        )
    }

    private fun periodsOverlap(
        left: EventMatchSide,
        right: EventMatchSide,
    ): Boolean {
        val dates = listOf(left.startDate, left.endDate, right.startDate, right.endDate)
        if (dates.any { it == null }) {
            return false
        }
        val leftStart = requireNotNull(left.startDate)
        val leftEnd = requireNotNull(left.endDate)
        val rightStart = requireNotNull(right.startDate)
        val rightEnd = requireNotNull(right.endDate)
        return !leftStart.isAfter(rightEnd) && !rightStart.isAfter(leftEnd)
    }

    private fun venuesMatch(
        left: String?,
        right: String?,
    ): Boolean {
        val normalizedLeft = EventTitleNormalizer.normalize(left.orEmpty())
        val normalizedRight = EventTitleNormalizer.normalize(right.orEmpty())
        return normalizedLeft.isNotEmpty() && normalizedLeft == normalizedRight
    }

    private fun prompt(
        left: EventMatchSide,
        right: EventMatchSide,
    ): String =
        "왼쪽 제목=${left.title} 기간=${left.startDate}~${left.endDate} 장소=${left.venue.orEmpty()} " +
            "contentId=${left.contentId} / 오른쪽 제목=${right.title} 기간=${right.startDate}~${right.endDate} " +
            "장소=${right.venue.orEmpty()} contentId=${right.contentId}"
}
