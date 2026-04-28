package com.sleekydz86.idolglow.productpackage.discovery.application.dto

import com.sleekydz86.idolglow.productpackage.product.domain.Product
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.math.RoundingMode

@Schema(description = "상품 랭킹 및 추천 응답 DTO")
data class ProductRankingResponse(
    @field:Schema(description = "상품 ID", example = "1")
    val id: Long,
    @field:Schema(description = "상품명", example = "헤어 메이크업 패키지")
    val name: String,
    @field:Schema(description = "상품 설명", example = "헤어와 메이크업 코스가 포함된 상품입니다.")
    val description: String,
    @field:Schema(description = "상품 기본가(옵션 합과 별도)", example = "10000.00")
    val basePrice: BigDecimal,
    @field:Schema(description = "옵션 가격 합(기본가 제외)", example = "90000.00")
    val optionsTotalPrice: BigDecimal,
    @field:Schema(description = "최소 가격", example = "100000.00")
    val minPrice: BigDecimal,
    @field:Schema(description = "총 가격", example = "300000.00")
    val totalPrice: BigDecimal,
    @field:Schema(description = "태그 목록", example = "[\"아이돌\", \"메이크업\"]")
    val tagNames: List<String>,
    @field:Schema(description = "찜 개수", example = "120")
    val wishCount: Long,
    @field:Schema(description = "평균 평점", example = "4.70")
    val averageRating: BigDecimal,
    @field:Schema(description = "리뷰 개수", example = "42")
    val reviewCount: Long,
    @field:Schema(
        description = "추천 신호와 겹친 상품 태그(찜/리뷰 선호 태그 및 설문 컨셉명과 일치하는 태그)",
        example = "[\"DREAMY\", \"메이크업\"]"
    )
    val matchedTags: List<String>,

    @field:Schema(description = "상품 대표 이미지 URL(없으면 null)")
    val thumbnailUrl: String? = null,
) {
    companion object {
        fun from(
            product: Product,
            tagNames: List<String>,
            wishCount: Long,
            averageRating: Double,
            reviewCount: Long,
            matchedTags: List<String> = emptyList(),
            thumbnailUrl: String? = null,
        ): ProductRankingResponse =
            ProductRankingResponse(
                id = product.id,
                name = product.name,
                description = product.description,
                basePrice = product.basePrice,
                optionsTotalPrice = product.optionsTotalPrice,
                minPrice = product.minPrice,
                totalPrice = product.totalPrice,
                tagNames = tagNames,
                wishCount = wishCount,
                averageRating = BigDecimal.valueOf(averageRating).setScale(2, RoundingMode.HALF_UP),
                reviewCount = reviewCount,
                matchedTags = matchedTags,
                thumbnailUrl = thumbnailUrl,
            )
    }
}
