package com.sleekydz86.idolglow.sitecontent.ui

import com.sleekydz86.idolglow.sitecontent.application.dto.SiteHomeContentResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.ResponseEntity

@Tag(name = "사이트 콘텐츠", description = "공개 사이트 홈·에셋 조회 API")
interface SiteContentApi {

    @Operation(
        summary = "홈 콘텐츠 조회",
        description = "홈 히어로 슬라이드, 배너, 팝업 등 구성 데이터를 반환합니다.",
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    fun readHomeContent(): ResponseEntity<SiteHomeContentResponse>

    @Operation(
        summary = "사이트 에셋(이미지) 조회",
        description = "배너·팝업·메인 슬라이드 이미지 등 객체 스토리지 경로의 바이너리를 반환합니다.",
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    fun readAsset(
        @Parameter(description = "객체 키(스토리지 경로)", example = "webzine/site-content/popups/sample.png")
        objectKey: String,
    ): ResponseEntity<ByteArrayResource>
}
