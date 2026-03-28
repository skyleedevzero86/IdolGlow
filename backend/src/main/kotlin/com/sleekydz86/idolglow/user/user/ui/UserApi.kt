package com.sleekydz86.idolglow.user.user.ui

import com.sleekydz86.idolglow.user.user.application.dto.GetUserLoginInfoResponse
import com.sleekydz86.idolglow.user.user.ui.request.UpdateProfileRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.multipart.MultipartFile

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
    fun updateProfile(
        @Parameter(hidden = true)
        userId: Long,
        @Valid request: UpdateProfileRequest
    ): ResponseEntity<GetUserLoginInfoResponse>

    @Operation(summary = "프로필 이미지 업로드", description = "JPEG/PNG/WebP, 최대 5MB. MinIO 또는 로컬 디스크에 저장 후 URL을 users에 반영합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "업로드 및 반영 성공"),
            ApiResponse(responseCode = "400", description = "형식/용량 오류"),
        ]
    )
    fun uploadProfileImage(
        @Parameter(hidden = true)
        userId: Long,
        @Parameter(description = "이미지 파일", required = true)
        file: MultipartFile,
    ): ResponseEntity<GetUserLoginInfoResponse>
}
