package com.sleekydz86.idolglow.survey.application

import com.sleekydz86.idolglow.productpackage.attraction.application.port.out.TourAttractionQueryPort
import com.sleekydz86.idolglow.productpackage.attraction.domain.SeoulDistrictTourCodeMapper
import com.sleekydz86.idolglow.productpackage.product.application.ProductQueryService
import com.sleekydz86.idolglow.productpackage.product.application.dto.ProductBrowseParams
import com.sleekydz86.idolglow.productpackage.product.domain.ProductSort
import com.sleekydz86.idolglow.survey.domain.dto.SurveyRecommendedAttractionResponse
import com.sleekydz86.idolglow.survey.domain.dto.SurveyRecommendationResponse
import com.sleekydz86.idolglow.survey.infrastructure.OpenAiSurveyRecommendationClient
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
    private val productQueryService: ProductQueryService,
    private val tourAttractionQueryPort: TourAttractionQueryPort,
    private val openAiSurveyRecommendationClient: OpenAiSurveyRecommendationClient,
) {
    fun generate(userId: Long, submissionId: Long, useLlm: Boolean): SurveyRecommendationResponse {
        val submission = surveySubmissionJpaRepository.findByIdAndUserId(submissionId, userId)
            ?: throw IllegalArgumentException("설문 제출 정보를 찾을 수 없습니다. submissionId=$submissionId")

        val answerHighlights = submission.answers.mapNotNull { answer ->
            answer.answerText?.trim()?.takeIf { it.isNotEmpty() } ?: answer.selectedOptions
                .map { it.optionText.trim() }
                .filter { it.isNotEmpty() }
                .joinToString(", ")
                .takeIf { it.isNotEmpty() }
        }

        val destinationHint = answerHighlights.firstOrNull()?.take(20) ?: "서울"
        val titleFallback = "설문 기반 ${destinationHint} 추천 코스"
        val subtitleFallback = "응답 취향과 관광 데이터 기반 맞춤 제안"
        val narrativeFallback = if (answerHighlights.isEmpty()) {
            "설문 응답을 기반으로 서울 인기 관광지와 상품을 조합한 추천 코스를 구성했습니다."
        } else {
            "설문 응답(${answerHighlights.take(3).joinToString(" / ")})을 반영해 관광지 동선과 상품 구성을 추천합니다."
        }

        val baseYm = YearMonth.now(ZoneId.of("Asia/Seoul")).minusMonths(1).format(DateTimeFormatter.ofPattern("yyyyMM"))
        val attractions = runCatching {
            tourAttractionQueryPort.fetchAreaBasedAttractions(
                baseYm = baseYm,
                areaCode = SeoulDistrictTourCodeMapper.SEOUL_AREA_CODE,
                signguCode = 11530,
                size = 30,
            )
        }.getOrDefault(emptyList()).take(5)

        val llm = if (useLlm) {
            openAiSurveyRecommendationClient.generate(
                titleFallback = titleFallback,
                subtitleFallback = subtitleFallback,
                narrativeFallback = narrativeFallback,
                answerHighlights = answerHighlights,
                attractions = attractions,
            )
        } else {
            null
        }

        val keyword = answerHighlights.firstOrNull()
        val products = productQueryService.browseProducts(
            ProductBrowseParams(
                lastId = null,
                offset = 0,
                size = 5,
                tag = null,
                tags = emptyList(),
                keyword = keyword,
                minPrice = null,
                maxPrice = null,
                visitDate = null,
                reservableOnly = false,
                sort = ProductSort.POPULARITY,
                nearLatitude = null,
                nearLongitude = null,
                radiusMeters = null,
            )
        ).items

        return SurveyRecommendationResponse(
            submissionId = submission.id,
            llmEnhanced = llm != null,
            title = llm?.title ?: titleFallback,
            subtitle = llm?.subtitle ?: subtitleFallback,
            narrative = llm?.narrative ?: narrativeFallback,
            attractions = attractions.map { attraction ->
                SurveyRecommendedAttractionResponse(
                    attractionCode = attraction.attractionCode,
                    name = attraction.name,
                    areaName = attraction.areaName,
                    signguName = attraction.signguName,
                    categoryLarge = attraction.categoryLarge,
                    categoryMiddle = attraction.categoryMiddle,
                    rank = attraction.rank,
                    reason = llm?.attractionReasons?.get(attraction.attractionCode)
                        ?: "${attraction.name}은(는) 최근 인기 상위 관광지로 여행 동선에 넣기 좋습니다.",
                )
            },
            recommendedProducts = products,
        )
    }
}
