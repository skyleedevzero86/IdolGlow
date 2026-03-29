package com.sleekydz86.idolglow.productpackage.product.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "상품 위치 정보 DTO")
data class ProductLocationPayload(
    @field:Schema(description = "장소명", example = "성수 메이크업 스튜디오")
    val name: String,
    @field:Schema(description = "위도", example = "37.5445")
    val latitude: BigDecimal,
    @field:Schema(description = "경도", example = "127.0557")
    val longitude: BigDecimal,
    @field:Schema(description = "도로명 주소", example = "서울 성동구 연무장길 1")
    val roadAddressName: String?,
    @field:Schema(description = "지번 주소", example = "서울 성동구 성수동1가 1-1")
    val addressName: String?,
    @field:Schema(description = "카카오 장소 ID", example = "123456789")
    val kakaoPlaceId: String,
)
