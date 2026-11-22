package com.sleekydz86.idolglow.survey.application

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class SurveyRecommendationWarmUpListener(
    private val surveyRecommendationService: SurveyRecommendationService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async("surveyLlmExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onSubmitted(event: SurveySubmittedEvent) {
        runCatching { surveyRecommendationService.prepare(event.userId, event.submissionId) }
            .onFailure { log.warn("설문 추천 미리 만들기 실패 submissionId={} error={}", event.submissionId, it.message) }
    }
}
