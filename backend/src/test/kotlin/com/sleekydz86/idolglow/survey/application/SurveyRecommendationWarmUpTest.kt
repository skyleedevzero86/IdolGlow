package com.sleekydz86.idolglow.survey.application

import com.sleekydz86.idolglow.productpackage.attraction.application.port.out.TourAttractionQueryPort
import com.sleekydz86.idolglow.productpackage.attraction.domain.TourAttraction
import com.sleekydz86.idolglow.productpackage.discovery.application.ProductDiscoveryService
import com.sleekydz86.idolglow.productpackage.discovery.application.RecommendationSignals
import com.sleekydz86.idolglow.survey.domain.SurveyAnswer
import com.sleekydz86.idolglow.survey.domain.SurveyQuestion
import com.sleekydz86.idolglow.survey.domain.SurveySubmission
import com.sleekydz86.idolglow.survey.infrastructure.SurveyLlmRecommendationStore
import com.sleekydz86.idolglow.survey.infrastructure.SurveySubmissionJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("설문 제출 직후 추천 미리 만들기")
class SurveyRecommendationWarmUpTest {
    private val submissions = mockk<SurveySubmissionJpaRepository>()
    private val tourAttractions = mockk<TourAttractionQueryPort>()
    private val store = mockk<SurveyLlmRecommendationStore>(relaxed = true)
    private val discovery = mockk<ProductDiscoveryService>()
    private val service = SurveyRecommendationService(submissions, discovery, tourAttractions, store)

    private val attraction =
        TourAttraction("A1", "경복궁", 1, "서울", 11530, "종로구", "관광지", "역사관광지", 1, null, null, "202610")

    private fun submission(
        id: Long,
        answerText: String,
    ): SurveySubmission {
        val question = mockk<SurveyQuestion>()
        every { question.recommendationSignal } returns null
        val answer = mockk<SurveyAnswer>()
        every { answer.question } returns question
        every { answer.answerText } returns answerText
        every { answer.selectedOptions } returns mutableListOf()
        val submission = mockk<SurveySubmission>()
        every { submission.id } returns id
        every { submission.answers } returns mutableListOf(answer)
        return submission
    }

    @DisplayName("제출 이벤트를 받으면 응답과 관광지로 LLM 작업을 미리 요청한다")
    @Test
    fun requestsLlmRightAfterSubmission() {
        every { submissions.findByIdAndUserId(10L, 1L) } returns submission(10L, "BTS")
        every { tourAttractions.fetchAreaBasedAttractions(any(), any(), any(), any()) } returns listOf(attraction)
        every { discovery.signalsOf(1L) } returns RecommendationSignals.EMPTY
        every { discovery.findRecommendedProducts(any<RecommendationSignals>(), any()) } returns emptyList()

        SurveyRecommendationWarmUpListener(service).onSubmitted(SurveySubmittedEvent(userId = 1L, submissionId = 10L))

        verify(exactly = 1) { store.findOrRequest(10L, listOf("BTS"), listOf(attraction), emptyList()) }
    }

    @DisplayName("다른 사용자의 제출이면 아무것도 요청하지 않는다")
    @Test
    fun ignoresOtherUsersSubmission() {
        every { submissions.findByIdAndUserId(10L, 2L) } returns null

        SurveyRecommendationWarmUpListener(service).onSubmitted(SurveySubmittedEvent(userId = 2L, submissionId = 10L))

        verify(exactly = 0) { store.findOrRequest(any(), any(), any(), any()) }
    }

    @DisplayName("제출한 설문이 없으면 최신 추천은 null이다")
    @Test
    fun latestIsNullWithoutSubmission() {
        every { submissions.findFirstByUserIdOrderByIdDesc(3L) } returns null

        assertNull(service.generateLatest(3L, useLlm = true))
    }
}
