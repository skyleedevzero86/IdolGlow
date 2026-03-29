package com.sleekydz86.idolglow.wish.ui

import com.sleekydz86.idolglow.wish.application.dto.WishToggleResponse
import com.sleekydz86.idolglow.wish.application.dto.WishedProductPagingResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity

@Tag(name = "Wish", description = "위시 관련 API")
interface WishApi {

    @Operation(
        summary = "위시 상태 설정",
        description = "위시 상태를 토글합니다. " +
                "이미 위시 리스트에 있는 경우 해제하고, 위시 리스트에 없는 경우 위시로 등록합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "위시 토글 성공"),
            ApiResponse(responseCode = "404", description = "회원 또는 상품을 찾을 수 없음")
        ]
    )
    fun toggleWish(
        @Parameter(hidden = true)
        userId: Long,
        @Parameter(description = "상품 ID", example = "1")
        productId: Long
    ): ResponseEntity<WishToggleResponse>

    @Operation(
        summary = "위시 상품 목록 조회",
        description = "no-offset 기반으로 위시 상품 목록을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    fun findWishes(
        @Parameter(hidden = true)
        userId: Long,
        @Parameter(description = "마지막 위시 상품 ID(커서). 지정 시 해당 ID보다 작은 상품부터 조회", example = "100")
        lastWishId: Long?,
        @Parameter(description = "페이지 크기. 미지정 시 20으로 설정", example = "20")
        size: Int?
    ): ResponseEntity<List<WishedProductPagingResponse>>
}
