package com.sleekydz86.idolglow.survey.infrastructure

import com.sleekydz86.idolglow.productpackage.attraction.domain.TourAttraction
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor

@Component
class SurveyLlmRecommendationStore(
    private val router: SurveyRecommendationLlmRouter,
    @Qualifier("surveyLlmExecutor")
    private val executor: Executor,
) {
    internal var clock: Clock = Clock.systemUTC()
    private val log = LoggerFactory.getLogger(javaClass)
    private val results: MutableMap<Long, LlmSurveyRecommendation> = Collections.synchronizedMap(boundedMap())
    private val failedAt = ConcurrentHashMap<Long, Instant>()
    private val pending: MutableSet<Long> = ConcurrentHashMap.newKeySet()

    fun findOrRequest(
        submissionId: Long,
        answerHighlights: List<String>,
        attractions: List<TourAttraction>,
        reasonSummaries: List<String> = emptyList(),
    ): LlmSurveyRecommendation? {
        val ready = results[submissionId]
        if (ready == null && canRequest(submissionId, attractions) && pending.add(submissionId)) {
            submit(submissionId, SurveyLlmInput(answerHighlights, attractions, reasonSummaries))
        }
        return ready
    }

    private fun canRequest(
        submissionId: Long,
        attractions: List<TourAttraction>,
    ): Boolean = attractions.isNotEmpty() && !recentlyFailed(submissionId)

    fun isPending(submissionId: Long): Boolean = submissionId in pending

    private fun submit(
        submissionId: Long,
        input: SurveyLlmInput,
    ) {
        runCatching {
            executor.execute { generate(submissionId, input) }
        }.onFailure {
            pending.remove(submissionId)
            log.warn("설문 추천 LLM 작업을 큐에 넣지 못함 submissionId={} error={}", submissionId, it.message)
        }
    }

    private fun generate(
        submissionId: Long,
        input: SurveyLlmInput,
    ) {
        try {
            val recommendation =
                runCatching {
                    router.generate(input.answerHighlights, input.attractions, input.reasonSummaries)
                }.getOrNull()
            if (recommendation == null) {
                failedAt[submissionId] = clock.instant()
            } else {
                results[submissionId] = recommendation
                failedAt.remove(submissionId)
            }
        } finally {
            pending.remove(submissionId)
        }
    }

    private fun recentlyFailed(submissionId: Long): Boolean =
        failedAt[submissionId]?.let { Duration.between(it, clock.instant()) < RETRY_AFTER } ?: false

    private fun boundedMap(): LinkedHashMap<Long, LlmSurveyRecommendation> =
        object : LinkedHashMap<Long, LlmSurveyRecommendation>() {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, LlmSurveyRecommendation>?): Boolean =
                size > MAX_RESULTS
        }

    private data class SurveyLlmInput(
        val answerHighlights: List<String>,
        val attractions: List<TourAttraction>,
        val reasonSummaries: List<String>,
    )

    companion object {
        private const val MAX_RESULTS = 1000
        private val RETRY_AFTER: Duration = Duration.ofMinutes(10)
    }
}
