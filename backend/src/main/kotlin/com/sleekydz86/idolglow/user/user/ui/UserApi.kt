package com.sleekydz86.idolglow.user.user.ui

import com.sleekydz86.idolglow.user.user.application.dto.GetUserLoginInfoResponse
import com.sleekydz86.idolglow.user.user.ui.request.UpdateNicknameRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity

@Tag(name = "User", description = "유저 관련 API")
interface UserApi {

    @Operation(
        summary = "닉네임 수정",
        description = "로그인한 사용자의 닉네임을 수정합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공"),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
                content = [Content(schema = Schema(hidden = true))]
            )
        ]
    )
    fun updateNickname(
        @Parameter(hidden = true)
        userId: Long,
        @Valid request: UpdateNicknameRequest
    ): ResponseEntity<GetUserLoginInfoResponse>
}
