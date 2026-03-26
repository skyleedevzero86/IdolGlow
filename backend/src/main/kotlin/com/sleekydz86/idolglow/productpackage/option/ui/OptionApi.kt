package com.sleekydz86.idolglow.productpackage.option.ui

import com.sleekydz86.idolglow.productpackage.option.application.dto.OptionResponse
import com.sleekydz86.idolglow.productpackage.option.ui.request.CreateOptionRequest
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.web.multipart.MultipartFile

@Tag(name = "Option", description = "옵션 관련 API")
interface OptionApi {

    @Operation(
        summary = "옵션 목록 조회",
        description = "옵션 목록을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    fun findOptions(): List<OptionResponse>

    @Operation(
        summary = "옵션 상세 조회",
        description = "특정 옵션의 상세 정보를 조회합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(
                responseCode = "404",
                description = "옵션을 찾을 수 없음",
                content = [Content(schema = Schema(hidden = true))]
            )
        ]
    )
    fun findOption(
        @Parameter(description = "옵션 ID", example = "1")
        optionId: Long
    ): OptionResponse

    @Operation(
        summary = "옵션 생성",
        description = "옵션을 생성합니다. (이미지는 선택)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "생성 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청")
        ]
    )
    fun createOption(
        @Parameter(description = "옵션 생성 요청 본문")
        @Valid request: CreateOptionRequest,
        @Parameter(
            description = "옵션 이미지 파일 목록",
            content = [
                Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    array = ArraySchema(schema = Schema(type = "string", format = "binary"))
                )
            ]
        )
        images: List<MultipartFile>?
    ): OptionResponse
}
