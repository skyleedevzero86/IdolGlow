package com.sleekydz86.idolglow.wish.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "위시 상태 설정 응답 DTO")
class WishToggleResponse(
    @field:Schema(description = "생성된 위시 ID", example = "1")
    val id: Long,
    @field:Schema(description = "위시 상태", example = "true")
    val wished: Boolean,
) {
    companion object {
        fun from(
            id: Long,
            wished: Boolean
        ): WishToggleResponse =
            WishToggleResponse(
                id = id,
                wished = wished
            )
    }
}