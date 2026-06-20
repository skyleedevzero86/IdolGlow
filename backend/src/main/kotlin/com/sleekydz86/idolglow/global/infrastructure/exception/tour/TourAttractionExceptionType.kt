package com.sleekydz86.idolglow.global.infrastructure.exception.tour

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomExceptionType

enum class TourAttractionExceptionType(
    override val errorCode: String,
    override val message: String,
    override val httpStatusCode: Int,
) : CustomExceptionType {
    PRODUCT_NOT_FOUND(
        "TOUR_ATTRACTION_PRODUCT_NOT_FOUND",
        "상품을 찾을 수 없습니다.",
        404,
    ),
    PRODUCT_LOCATION_NOT_FOUND(
        "TOUR_ATTRACTION_PRODUCT_LOCATION_NOT_FOUND",
        "상품 위치 정보가 없어 주변 관광지를 조회할 수 없습니다.",
        400,
    ),
    DISTRICT_NOT_SUPPORTED(
        "TOUR_ATTRACTION_DISTRICT_NOT_SUPPORTED",
        "서울 자치구 정보를 찾지 못해 주변 관광지를 조회할 수 없습니다.",
        400,
    ),
    INVALID_BASE_YM(
        "INVALID_BASE_YM",
        "base_ym 형식이 올바르지 않습니다. YYYYMM 형식을 사용해 주세요.",
        400,
    ),
    TOUR_API_KEY_MISSING(
        "TOUR_API_KEY_MISSING",
        "관광 API 서비스 키가 설정되지 않았습니다.",
        500,
    ),
    TOUR_API_CALL_FAILED(
        "TOUR_API_CALL_FAILED",
        "외부 관광 API 호출에 실패했습니다.",
        502,
    ),
    TOUR_API_BAD_RESPONSE(
        "TOUR_API_BAD_RESPONSE",
        "외부 관광 API 응답 형식이 올바르지 않습니다.",
        502,
    ),
    TOUR_API_ERROR(
        "TOUR_API_ERROR",
        "외부 관광 API가 오류를 반환했습니다.",
        502,
    ),
}
