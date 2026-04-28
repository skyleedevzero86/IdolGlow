package com.sleekydz86.idolglow.productpackage.attraction.domain.dto

import io.swagger.v3.oas.annotations.media.Schema

data class ProductTourAttractionResponse(
    @field:Schema(description = "상품 ID", example = "7")
    val productId: Long,
    @field:Schema(description = "상품명", example = "뷰티 올인원 패키지")
    val productName: String,
    @field:Schema(description = "상품 위치 기준 자치구", example = "구로구")
    val district: String,
    @field:Schema(description = "지역 코드(areaCd)", example = "11")
    val areaCode: Int,
    @field:Schema(description = "시군구 코드(signguCd)", example = "11530")
    val signguCode: Int,
    @field:Schema(description = "조회 기준연월(YYYYMM)", example = "202503")
    val baseYm: String,
    @field:Schema(description = "추천 관광지 목록")
    val attractions: List<ProductTourAttractionItemResponse>,
)

data class ProductTourAttractionItemResponse(
    @field:Schema(description = "관광지 코드", example = "b5ef6787d594080cd54b65a9bc884a9b")
    val attractionCode: String,
    @field:Schema(description = "관광지명", example = "NC백화점/신구로점")
    val name: String,
    @field:Schema(description = "지역명", example = "서울특별시")
    val areaName: String?,
    @field:Schema(description = "시군구명", example = "구로구")
    val signguName: String?,
    @field:Schema(description = "카테고리 대분류", example = "관광지")
    val categoryLarge: String?,
    @field:Schema(description = "카테고리 중분류", example = "쇼핑")
    val categoryMiddle: String?,
    @field:Schema(description = "중심지 순위", example = "1")
    val rank: Int,
    @field:Schema(description = "경도(mapX)", example = "126.882825790362")
    val mapX: Double?,
    @field:Schema(description = "위도(mapY)", example = "37.501164213239")
    val mapY: Double?,
    @field:Schema(description = "내부 추천 점수", example = "964")
    val score: Int,
    @field:Schema(description = "추천 사유", example = "상품 이용 전후 동선에 맞는 상위권 쇼핑 관광지입니다.")
    val reason: String,
)
