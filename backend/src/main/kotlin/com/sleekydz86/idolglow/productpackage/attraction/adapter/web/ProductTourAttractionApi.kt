package com.sleekydz86.idolglow.productpackage.attraction.ui

import com.sleekydz86.idolglow.productpackage.attraction.domain.dto.ProductTourAttractionResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity

@Tag(name = "[사용자 전용] 상품 관광지 추천", description = "상품 기준 주변/연계 관광지 추천 API")
interface ProductTourAttractionApi {
    @Operation(
        summary = "상품 주변 관광지 추천 조회",
        description = "TourAPI를 직접 조회해 추천 관광지를 반환합니다. areaCode/signguCode 미지정 시 기본값(서울/구로구)을 사용합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ProductTourAttractionResponse::class),
                        examples = [
                            ExampleObject(
                                name = "상품 주변 관광지 추천 응답",
                                value = """
                                {
                                  "productId": 7,
                                  "productName": "Glow 웨딩 메이크업 패키지",
                                  "district": "구로구",
                                  "areaCode": 11,
                                  "signguCode": 11530,
                                  "baseYm": "202504",
                                  "attractions": [
                                    {
                                      "attractionCode": "b5ef6787d594080cd54b65a9bc884a9b",
                                      "name": "NC백화점/신구로점",
                                      "areaName": "서울특별시",
                                      "signguName": "구로구",
                                      "categoryLarge": "관광지",
                                      "categoryMiddle": "쇼핑",
                                      "rank": 1,
                                      "mapX": 126.882825790362,
                                      "mapY": 37.501164213239,
                                      "score": 964,
                                      "reason": "구로구 내 쇼핑 카테고리 상위권 관광지로 상품 이용 전후 동선에 적합합니다."
                                    },
                                    {
                                      "attractionCode": "23afdc6f34468caf6f765034fd12e901",
                                      "name": "디큐브아트센터",
                                      "areaName": "서울특별시",
                                      "signguName": "구로구",
                                      "categoryLarge": "관광지",
                                      "categoryMiddle": "문화시설",
                                      "rank": 4,
                                      "mapX": 126.889241804891,
                                      "mapY": 37.508691159691,
                                      "score": 921,
                                      "reason": "구로구 내 문화시설 카테고리 상위권 관광지로 상품 이용 전후 동선에 적합합니다."
                                    }
                                  ]
                                }
                                """
                            )
                        ]
                    )
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
                content = [
                    Content(
                        mediaType = "application/json",
                        examples = [
                            ExampleObject(
                                name = "base_ym 형식 오류",
                                value = """{"name":"INVALID_BASE_YM","message":"base_ym 형식이 올바르지 않습니다. YYYYMM 형식을 사용해 주세요.","errorCode":"INVALID_BASE_YM"}"""
                            )
                        ]
                    )
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "상품 없음",
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "502",
                description = "외부 관광 API 오류",
                content = [Content(schema = Schema(hidden = true))]
            ),
        ]
    )
    fun findTourAttractions(
        @Parameter(description = "상품 ID", example = "7")
        productId: Long,
        @Parameter(description = "조회 개수(1~1000). 기본값 10", example = "10")
        size: Int?,
        @Parameter(description = "기준연월(YYYYMM). 미지정 시 전월", example = "202503")
        baseYm: String?,
        @Parameter(description = "지역 코드(areaCd). 미지정 시 11(서울)", example = "11")
        areaCode: Int?,
        @Parameter(description = "시군구 코드(signguCd). 미지정 시 11530(구로구)", example = "11530")
        signguCode: Int?,
        @Parameter(description = "카테고리 필터(대/중분류 부분일치)", example = "쇼핑")
        category: String?,
    ): ResponseEntity<ProductTourAttractionResponse>
}
