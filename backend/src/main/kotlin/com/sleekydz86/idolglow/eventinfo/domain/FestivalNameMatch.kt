package com.sleekydz86.idolglow.eventinfo.domain

object FestivalNameMatch {
    fun findSingle(
        target: FestivalEvent,
        candidates: List<FestivalEvent>,
    ): FestivalEvent? {
        val name = KoreanNameInParentheses.extract(target.title)?.let(EventTitleNormalizer::normalize)
        if (name.isNullOrEmpty() || !hasPeriod(target)) return null
        return candidates
            .filter { hasPeriod(it) && EventTitleNormalizer.normalize(it.title) == name && overlaps(target, it) }
            .singleOrNull()
    }

    private fun hasPeriod(event: FestivalEvent): Boolean = !event.eventStartDate.isNullOrBlank() && !event.eventEndDate.isNullOrBlank()

    private fun overlaps(
        first: FestivalEvent,
        second: FestivalEvent,
    ): Boolean = first.eventStartDate!! <= second.eventEndDate!! && second.eventStartDate!! <= first.eventEndDate!!
}
