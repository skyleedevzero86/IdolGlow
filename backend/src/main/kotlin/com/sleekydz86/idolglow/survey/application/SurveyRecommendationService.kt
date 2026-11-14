package com.sleekydz86.idolglow.survey.application

import com.sleekydz86.idolglow.productpackage.attraction.application.port.out.TourAttractionQueryPort
import com.sleekydz86.idolglow.productpackage.attraction.domain.SeoulDistrictTourCodeMapper
import com.sleekydz86.idolglow.productpackage.attraction.domain.TourAttraction
import com.sleekydz86.idolglow.productpackage.discovery.application.ConceptAliases
import com.sleekydz86.idolglow.productpackage.discovery.application.ProductDiscoveryService
import com.sleekydz86.idolglow.productpackage.discovery.application.RecommendationSignals
import com.sleekydz86.idolglow.productpackage.discovery.application.dto.ProductRankingResponse
import com.sleekydz86.idolglow.survey.domain.SurveySubmission
import com.sleekydz86.idolglow.survey.domain.dto.SurveyRecommendationResponse
import com.sleekydz86.idolglow.survey.domain.dto.SurveyRecommendedAttractionResponse
import com.sleekydz86.idolglow.survey.domain.dto.SurveyUsedSignalResponse
import com.sleekydz86.idolglow.survey.domain.dto.SurveyUsedSignalSource
import com.sleekydz86.idolglow.survey.domain.dto.SurveyUsedSignalType
import com.sleekydz86.idolglow.survey.infrastructure.SurveyLlmRecommendationStore
import com.sleekydz86.idolglow.survey.infrastructure.SurveySubmissionJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Transactional(readOnly = true)
@Service
class SurveyRecommendationService(
    private val surveySubmissionJpaRepository: SurveySubmissionJpaRepository,
    private val productDiscoveryService: ProductDiscoveryService,
    private val tourAttractionQueryPort: TourAttractionQueryPort,
    private val surveyLlmRecommendationStore: SurveyLlmRecommendationStore,
) {
    fun generate(
        userId: Long,
        submissionId: Long,
        useLlm: Boolean,
    ): SurveyRecommendationResponse {
        val submission =
            surveySubmissionJpaRepository.findByIdAndUserId(submissionId, userId)
                ?: throw IllegalArgumentException("설문 제출 정보를 찾을 수 없습니다. submissionId=$submissionId")

        val answerHighlights = answerHighlights(submission)
        val plan = plan(userId, submission)

        val destinationHint = answerHighlights.firstOrNull()?.take(DESTINATION_HINT_LENGTH) ?: "서울"
        val narrativeFallback =
            if (answerHighlights.isEmpty()) {
                "설문 응답을 기반으로 서울 인기 관광지와 상품을 조합한 추천 코스를 구성했습니다."
            } else {
                "설문 응답(${answerHighlights.take(NARRATIVE_HIGHLIGHT_COUNT).joinToString(" / ")})을 반영해 관광지 동선과 상품 구성을 추천합니다."
            }

        val llm =
            if (useLlm) {
                surveyLlmRecommendationStore.findOrRequest(
                    submission.id,
                    answerHighlights,
                    plan.attractions,
                    plan.reasonSummaries,
                )
            } else {
                null
            }

        return SurveyRecommendationResponse(
            submissionId = submission.id,
            llmEnhanced = llm != null,
            llmPending = useLlm && llm == null && surveyLlmRecommendationStore.isPending(submission.id),
            title = llm?.title ?: "설문 기반 $destinationHint 추천 코스",
            subtitle = llm?.subtitle ?: "응답 취향과 관광 데이터 기반 맞춤 제안",
            narrative = llm?.narrative ?: narrativeFallback,
            attractions =
                plan.attractions.map { attraction ->
                    SurveyRecommendedAttractionResponse(
                        attractionCode = attraction.attractionCode,
                        name = attraction.name,
                        areaName = attraction.areaName,
                        signguName = attraction.signguName,
                        categoryLarge = attraction.categoryLarge,
                        categoryMiddle = attraction.categoryMiddle,
                        rank = attraction.rank,
                        reason =
                            llm?.attractionReasons?.get(attraction.attractionCode)
                                ?: "${attraction.name}은(는) 최근 인기 상위 관광지로 여행 동선에 넣기 좋습니다.",
                    )
                },
            recommendedProducts = plan.products,
            usedSignals = plan.usedSignals,
        )
    }

    fun generateLatest(
        userId: Long,
        useLlm: Boolean,
    ): SurveyRecommendationResponse? =
        surveySubmissionJpaRepository
            .findFirstByUserIdOrderByIdDesc(userId)
            ?.let { generate(userId, it.id, useLlm) }

    fun prepare(
        userId: Long,
        submissionId: Long,
    ) {
        val submission = surveySubmissionJpaRepository.findByIdAndUserId(submissionId, userId) ?: return
        val plan = plan(userId, submission)
        surveyLlmRecommendationStore.findOrRequest(
            submission.id,
            answerHighlights(submission),
            plan.attractions,
            plan.reasonSummaries,
        )
    }

    private fun plan(
        userId: Long,
        submission: SurveySubmission,
    ): RecommendationPlan {
        val formSignals = SurveySignalExtractor.extract(submission)
        val userSignals = productDiscoveryService.signalsOf(userId)
        val products = productDiscoveryService.findRecommendedProducts(userSignals.merge(formSignals), PRODUCT_SIZE)
        return RecommendationPlan(
            products = products,
            attractions = attractions(formSignals.placeKeywords + userSignals.placeKeywords),
            usedSignals =
                usedSignals(formSignals, SurveyUsedSignalSource.FORM) +
                    usedSignals(userSignals, SurveyUsedSignalSource.USER_SURVEY),
            reasonSummaries = reasonSummaries(products),
        )
    }

    private fun usedSignals(
        signals: RecommendationSignals,
        source: SurveyUsedSignalSource,
    ): List<SurveyUsedSignalResponse> {
        val visitStart = signals.visitStart
        val visitEnd = signals.visitEnd
        val period = if (visitStart != null && visitEnd != null) listOf("$visitStart ~ $visitEnd") else emptyList()
        val concepts = signals.concepts().map { ConceptAliases.labelOf(it) ?: it.name }
        val activity = SurveyUsedSignalSource.ACTIVITY
        return listOf(
            SurveyUsedSignalResponse(SurveyUsedSignalType.CONCEPT, source, concepts),
            SurveyUsedSignalResponse(SurveyUsedSignalType.IDOL, source, signals.idolTokens),
            SurveyUsedSignalResponse(SurveyUsedSignalType.PLACE, source, signals.placeKeywords),
            SurveyUsedSignalResponse(SurveyUsedSignalType.TAG, source, signals.extraTags.toList()),
            SurveyUsedSignalResponse(SurveyUsedSignalType.VISIT_PERIOD, source, period),
            SurveyUsedSignalResponse(SurveyUsedSignalType.BEHAVIOR_TAG, activity, signals.behaviorTags.toList()),
        ).filter { it.values.isNotEmpty() }
    }

    private fun reasonSummaries(products: List<ProductRankingResponse>): List<String> =
        products
            .filter { it.reasons.isNotEmpty() }
            .map { product -> "${product.name}: ${product.reasons.joinToString(" / ") { it.label }}" }

    private fun answerHighlights(submission: SurveySubmission): List<String> =
        submission.answers.mapNotNull { answer ->
            answer.answerText?.trim()?.takeIf { it.isNotEmpty() } ?: answer.selectedOptions
                .map { it.optionText.trim() }
                .filter { it.isNotEmpty() }
                .joinToString(", ")
                .takeIf { it.isNotEmpty() }
        }

    private fun attractions(placeKeywords: List<String>): List<TourAttraction> {
        val baseYm = YearMonth.now(ZoneId.of("Asia/Seoul")).minusMonths(1).format(DateTimeFormatter.ofPattern("yyyyMM"))
        return runCatching {
            tourAttractionQueryPort.fetchAreaBasedAttractions(
                baseYm = baseYm,
                areaCode = SeoulDistrictTourCodeMapper.SEOUL_AREA_CODE,
                signguCode = districtCode(placeKeywords),
                size = ATTRACTION_FETCH_SIZE,
            )
        }.getOrDefault(emptyList()).take(ATTRACTION_SIZE)
    }

    private fun districtCode(placeKeywords: List<String>): Int? =
        placeKeywords.firstNotNullOfOrNull { keyword ->
            val district = SeoulDistrictTourCodeMapper.resolveDistrictLabel(keyword) ?: keyword
            SeoulDistrictTourCodeMapper.signguCodeOf(district)
        }

    private data class RecommendationPlan(
        val products: List<ProductRankingResponse>,
        val attractions: List<TourAttraction>,
        val usedSignals: List<SurveyUsedSignalResponse>,
        val reasonSummaries: List<String>,
    )

    companion object {
        private const val PRODUCT_SIZE = 5
        private const val ATTRACTION_FETCH_SIZE = 30
        private const val ATTRACTION_SIZE = 5
        private const val DESTINATION_HINT_LENGTH = 20
        private const val NARRATIVE_HIGHLIGHT_COUNT = 3
    }
}
