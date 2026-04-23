package com.sleekydz86.idolglow.productpackage.attraction.application

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.tour.TourAttractionExceptionType
import com.sleekydz86.idolglow.productpackage.attraction.application.port.out.TourAttractionQueryPort
import com.sleekydz86.idolglow.productpackage.attraction.domain.SeoulDistrictTourCodeMapper
import com.sleekydz86.idolglow.productpackage.attraction.domain.TourAttraction
import com.sleekydz86.idolglow.productpackage.attraction.domain.dto.ProductTourAttractionItemResponse
import com.sleekydz86.idolglow.productpackage.attraction.domain.dto.ProductTourAttractionResponse
import com.sleekydz86.idolglow.productpackage.product.domain.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Transactional(readOnly = true)
@Service
class ProductTourAttractionQueryService(
    private val productRepository: ProductRepository,
    private val tourAttractionQueryPort: TourAttractionQueryPort,
) {
    fun findAttractionsByProduct(
        productId: Long,
        size: Int,
        baseYm: String?,
        category: String?,
    ): ProductTourAttractionResponse {
        val product = productRepository.findById(productId)
            ?: throw CustomException(TourAttractionExceptionType.PRODUCT_NOT_FOUND)

        val location = product.productLocation
            ?: throw CustomException(TourAttractionExceptionType.PRODUCT_LOCATION_NOT_FOUND)

        val district = resolveDistrict(location.roadAddressName, location.addressName, location.name)
        val signguCode = SeoulDistrictTourCodeMapper.signguCodeOf(district)
            ?: throw CustomException(TourAttractionExceptionType.DISTRICT_NOT_SUPPORTED)
        val areaCode = SeoulDistrictTourCodeMapper.SEOUL_AREA_CODE
        val resolvedBaseYm = resolveBaseYm(baseYm)
        val resolvedSize = size.coerceIn(1, 30)

        val normalizedCategory = category?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        val tagNames = product.productTags.map { it.tagName.lowercase() }.toSet()

        val scored = tourAttractionQueryPort.fetchAreaBasedAttractions(
            baseYm = resolvedBaseYm,
            areaCode = areaCode,
            signguCode = signguCode,
            size = 50,
        ).asSequence()
            .filter { attraction ->
                normalizedCategory == null ||
                    attraction.categoryLarge.orEmpty().lowercase().contains(normalizedCategory) ||
                    attraction.categoryMiddle.orEmpty().lowercase().contains(normalizedCategory)
            }
            .map { attraction -> ScoredAttraction(attraction, computeScore(attraction, tagNames)) }
            .sortedWith(compareByDescending<ScoredAttraction> { it.score }.thenBy { it.attraction.rank }.thenBy { it.attraction.name })
            .take(resolvedSize)
            .toList()

        return ProductTourAttractionResponse(
            productId = product.id,
            productName = product.name,
            district = district,
            areaCode = areaCode,
            signguCode = signguCode,
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
                    reason = defaultReason(attraction, district),
                )
            }
        )
    }

    private fun resolveDistrict(vararg sources: String?): String {
        for (source in sources) {
            val candidate = source?.trim().orEmpty()
            if (candidate.isEmpty()) continue
            val matched = SeoulDistrictTourCodeMapper.resolveDistrictLabel(candidate)
            if (!matched.isNullOrBlank()) return matched
        }
        throw CustomException(TourAttractionExceptionType.DISTRICT_NOT_SUPPORTED)
    }

    private fun resolveBaseYm(baseYm: String?): String {
        val resolved = baseYm?.trim()?.takeIf { it.isNotEmpty() }
            ?: YearMonth.now(ZoneId.of("Asia/Seoul")).minusMonths(1).format(DateTimeFormatter.ofPattern("yyyyMM"))
        if (!BASE_YM_PATTERN.matches(resolved)) {
            throw CustomException(TourAttractionExceptionType.INVALID_BASE_YM)
        }
        return resolved
    }

    private fun computeScore(attraction: TourAttraction, tagNames: Set<String>): Int {
        val rankScore = 1000 - attraction.rank.coerceAtLeast(1)
        val categoryText = "${attraction.categoryLarge.orEmpty()} ${attraction.categoryMiddle.orEmpty()}".lowercase()
        val affinityScore = when {
            tagNames.any { it.contains("beauty") || it.contains("fashion") } && categoryText.contains("쇼핑") -> 120
            tagNames.any { it.contains("culture") || it.contains("art") } && categoryText.contains("문화") -> 110
            tagNames.any { it.contains("nature") || it.contains("healing") } && (categoryText.contains("자연") || categoryText.contains("공원")) -> 110
            tagNames.any { categoryText.contains(it) } -> 70
            else -> 0
        }
        return rankScore + affinityScore
    }

    private fun defaultReason(attraction: TourAttraction, district: String): String {
        val middle = attraction.categoryMiddle?.trim().takeUnless { it.isNullOrBlank() } ?: "관광"
        return "$district 내 $middle 카테고리 상위권 관광지로 상품 이용 전후 동선에 적합합니다."
    }

    private data class ScoredAttraction(val attraction: TourAttraction, val score: Int)

    companion object {
        private val BASE_YM_PATTERN = Regex("^\\d{6}$")
    }
}
