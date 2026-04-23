package com.sleekydz86.idolglow.productpackage.attraction.application

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.tour.TourAttractionExceptionType
import com.sleekydz86.idolglow.productpackage.attraction.application.port.out.TourAttractionQueryPort
import com.sleekydz86.idolglow.productpackage.attraction.domain.SeoulDistrictTourCodeMapper
import com.sleekydz86.idolglow.productpackage.attraction.domain.TourAttraction
import com.sleekydz86.idolglow.productpackage.attraction.domain.dto.ProductTourAttractionItemResponse
import com.sleekydz86.idolglow.productpackage.attraction.domain.dto.ProductTourAttractionResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Transactional(readOnly = true)
@Service
class ProductTourAttractionQueryService(
    private val tourAttractionQueryPort: TourAttractionQueryPort,
) {
    fun findAttractionsByProduct(
        productId: Long,
        size: Int,
        baseYm: String?,
        category: String?,
        areaCode: Int?,
        signguCode: Int?,
    ): ProductTourAttractionResponse {
        val resolvedAreaCode = areaCode ?: SeoulDistrictTourCodeMapper.SEOUL_AREA_CODE
        val resolvedSignguCode = signguCode ?: DEFAULT_SIGNGU_CODE
        val district = SeoulDistrictTourCodeMapper.districtOf(resolvedSignguCode)
            ?: "signgu-$resolvedSignguCode"
        val resolvedBaseYm = resolveBaseYm(baseYm)
        val resolvedSize = size.coerceIn(1, MAX_RESULT_SIZE)

        val normalizedCategory = category?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }

        val scored = tourAttractionQueryPort.fetchAreaBasedAttractions(
            baseYm = resolvedBaseYm,
            areaCode = resolvedAreaCode,
            signguCode = resolvedSignguCode,
            size = TOUR_API_NUM_OF_ROWS,
        ).asSequence()
            .filter { attraction ->
                normalizedCategory == null ||
                    attraction.categoryLarge.orEmpty().lowercase().contains(normalizedCategory) ||
                    attraction.categoryMiddle.orEmpty().lowercase().contains(normalizedCategory)
            }
            .map { attraction -> ScoredAttraction(attraction, computeScore(attraction)) }
            .sortedWith(compareByDescending<ScoredAttraction> { it.score }.thenBy { it.attraction.rank }.thenBy { it.attraction.name })
            .take(resolvedSize)
            .toList()

        return ProductTourAttractionResponse(
            productId = productId,
            productName = "외부 TourAPI 기반 관광지 추천",
            district = district,
            areaCode = resolvedAreaCode,
            signguCode = resolvedSignguCode,
            baseYm = resolvedBaseYm,
            attractions = scored.map { scoredAttraction ->
                val attraction = scoredAttraction.attraction
                ProductTourAttractionItemResponse(
                    attractionCode = attraction.attractionCode,
                    name = attraction.name,
                    areaName = attraction.areaName,
                    signguName = attraction.signguName,
                    categoryLarge = attraction.categoryLarge,
                    categoryMiddle = attraction.categoryMiddle,
                    rank = attraction.rank,
                    mapX = attraction.mapX,
                    mapY = attraction.mapY,
                    score = scoredAttraction.score,
                    reason = defaultReason(attraction, district, resolvedBaseYm),
                )
            }
        )
    }

    private fun resolveBaseYm(baseYm: String?): String {
        val resolved = baseYm?.trim()?.takeIf { it.isNotEmpty() }
            ?: YearMonth.now(ZoneId.of("Asia/Seoul")).minusMonths(1).format(DateTimeFormatter.ofPattern("yyyyMM"))
        if (!BASE_YM_PATTERN.matches(resolved)) {
            throw CustomException(TourAttractionExceptionType.INVALID_BASE_YM)
        }
        return resolved
    }

    private fun computeScore(attraction: TourAttraction): Int {
        val rankScore = 1000 - attraction.rank.coerceAtLeast(1)
        return rankScore
    }

    private fun defaultReason(attraction: TourAttraction, district: String, baseYm: String): String {
        val middle = attraction.categoryMiddle?.trim().takeUnless { it.isNullOrBlank() } ?: "관광"
        return "$district 내 $middle 카테고리 상위권 관광지입니다. 데이터 기준월: $baseYm"
    }

    private data class ScoredAttraction(val attraction: TourAttraction, val score: Int)

    companion object {
        private val BASE_YM_PATTERN = Regex("^\\d{6}$")
        private const val MAX_RESULT_SIZE = 1000
        private const val TOUR_API_NUM_OF_ROWS = 1000
        private const val DEFAULT_SIGNGU_CODE = 11530
    }
}
