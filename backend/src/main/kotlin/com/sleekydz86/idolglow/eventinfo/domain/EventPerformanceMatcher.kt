package com.sleekydz86.idolglow.eventinfo.domain

object EventPerformanceMatcher {
    const val CONFIRMED_SCORE: Int = 100
    const val CANDIDATE_SCORE: Int = 60

    fun decide(
        item: CollectedPerformance,
        masters: List<EventMaster>,
    ): EventMatchDecision {
        val scored =
            masters
                .map { master -> master to score(item, master) }
                .filter { (_, value) -> value > 0 }
        val confirmed = scored.filter { (_, value) -> value >= CONFIRMED_SCORE }
        return when {
            confirmed.size == 1 ->
                EventMatchDecision.Attach(confirmed[0].first.globalId, EventMatchStatus.CONFIRMED, CONFIRMED_SCORE)
            confirmed.size > 1 -> EventMatchDecision.Hold(EventMatchStatus.CONFLICT, CONFIRMED_SCORE)
            scored.size == 1 ->
                EventMatchDecision.Attach(scored[0].first.globalId, EventMatchStatus.CANDIDATE, scored[0].second)
            scored.size > 1 -> EventMatchDecision.Hold(EventMatchStatus.CONFLICT, scored.maxOf { (_, value) -> value })
            else -> EventMatchDecision.Fresh
        }
    }

    private fun score(
        item: CollectedPerformance,
        master: EventMaster,
    ): Int {
        val sameTitle = EventTitleNormalizer.normalize(item.title) == master.normalizedTitle
        if (!sameTitle || !periodsOverlap(item, master)) {
            return 0
        }
        return if (venuesMatch(item.venue, master.venue)) CONFIRMED_SCORE else CANDIDATE_SCORE
    }

    private fun periodsOverlap(
        item: CollectedPerformance,
        master: EventMaster,
    ): Boolean {
        val start = item.startDate
        val end = item.endDate
        val masterStart = master.startDate
        val masterEnd = master.endDate
        val comparable = listOf(start, end, masterStart, masterEnd).all { it != null }
        if (!comparable) {
            return false
        }
        val itemStart = requireNotNull(start)
        val itemEnd = requireNotNull(end)
        val otherStart = requireNotNull(masterStart)
        val otherEnd = requireNotNull(masterEnd)
        return !itemStart.isAfter(otherEnd) && !otherStart.isAfter(itemEnd)
    }

    private fun venuesMatch(
        left: String?,
        right: String?,
    ): Boolean {
        val normalizedLeft = EventTitleNormalizer.normalize(left.orEmpty())
        val normalizedRight = EventTitleNormalizer.normalize(right.orEmpty())
        return normalizedLeft.isNotEmpty() && normalizedLeft == normalizedRight
    }
}

sealed class EventMatchDecision {
    data class Attach(
        val globalId: String,
        val status: EventMatchStatus,
        val score: Int,
    ) : EventMatchDecision()

    data class Hold(
        val status: EventMatchStatus,
        val score: Int,
    ) : EventMatchDecision()

    data object Fresh : EventMatchDecision()
}
