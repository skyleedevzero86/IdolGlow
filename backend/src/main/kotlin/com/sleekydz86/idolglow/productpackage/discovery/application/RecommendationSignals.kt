package com.sleekydz86.idolglow.productpackage.discovery.application

import com.sleekydz86.idolglow.user.user.domain.UserSurvey
import com.sleekydz86.idolglow.user.user.domain.vo.ConceptType
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class RecommendationSignals(
    val conceptTags: Set<String> = emptySet(),
    val idolTokens: List<String> = emptyList(),
    val placeKeywords: List<String> = emptyList(),
    val extraTags: Set<String> = emptySet(),
    val behaviorTags: Set<String> = emptySet(),
    val visitStart: LocalDate? = null,
    val visitEnd: LocalDate? = null,
) {
    fun isEmpty(): Boolean =
        conceptTags.isEmpty() &&
            idolTokens.isEmpty() &&
            placeKeywords.isEmpty() &&
            extraTags.isEmpty() &&
            behaviorTags.isEmpty()

    fun merge(other: RecommendationSignals): RecommendationSignals {
        val keepOwnDates = visitStart != null && visitEnd != null
        return RecommendationSignals(
            conceptTags = conceptTags + other.conceptTags,
            idolTokens = distinctIgnoringCase(idolTokens + other.idolTokens),
            placeKeywords = distinctIgnoringCase(placeKeywords + other.placeKeywords),
            extraTags = extraTags + other.extraTags,
            behaviorTags = behaviorTags + other.behaviorTags,
            visitStart = if (keepOwnDates) visitStart else other.visitStart,
            visitEnd = if (keepOwnDates) visitEnd else other.visitEnd,
        )
    }

    fun concepts(): List<ConceptType> = conceptTags.mapNotNull(ConceptAliases::resolveName)

    fun conceptAliasTags(): List<String> = concepts().flatMap(ConceptAliases::aliasesOf).distinct()

    fun candidateTags(): List<String> = (behaviorTags + conceptAliasTags() + extraTags).distinct()

    fun tripDayCount(): Int {
        val start = visitStart
        val end = visitEnd
        if (start == null || end == null || end.isBefore(start)) return 0
        return ChronoUnit.DAYS.between(start, end).toInt() + 1
    }

    companion object {
        private const val MAX_IDOL_TOKENS = 8
        private const val MIN_IDOL_TOKEN_LENGTH = 2
        private val idolSeparators = Regex("[\\s,]+")

        val EMPTY = RecommendationSignals()

        fun fromUserSurvey(
            survey: UserSurvey?,
            behaviorTags: Set<String>,
        ): RecommendationSignals {
            if (survey == null) {
                return RecommendationSignals(behaviorTags = behaviorTags)
            }
            return RecommendationSignals(
                conceptTags = setOfNotNull(survey.concept.takeIf { ConceptAliases.aliasesOf(it).isNotEmpty() }?.name),
                idolTokens = idolTokens(survey.idolName),
                placeKeywords = keywords(survey.places),
                behaviorTags = behaviorTags,
                visitStart = survey.visitStartDate,
                visitEnd = survey.visitEndDate,
            )
        }

        fun idolTokens(raw: String): List<String> =
            distinctIgnoringCase(
                raw
                    .split(idolSeparators)
                    .map { it.trim() }
                    .filter { it.length >= MIN_IDOL_TOKEN_LENGTH },
            ).take(MAX_IDOL_TOKENS)

        fun keywords(raw: List<String>): List<String> {
            val cleaned = raw.map { it.trim() }.filter { it.isNotEmpty() }
            return distinctIgnoringCase(cleaned)
        }

        private fun distinctIgnoringCase(values: List<String>): List<String> = values.distinctBy { it.lowercase() }
    }
}
