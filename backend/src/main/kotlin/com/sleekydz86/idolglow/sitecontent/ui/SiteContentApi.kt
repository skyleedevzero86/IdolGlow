package com.sleekydz86.idolglow.sitecontent.ui

import com.sleekydz86.idolglow.sitecontent.application.dto.SiteHomeContentResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity

@Tag(name = "Site content", description = "홈 화면 공개 콘텐츠 API")
interface SiteContentApi {

    @Operation(
        summary = "홈 화면 콘텐츠 조회",
        description = "메인 슬라이드, 배너, 팝업 목록을 홈 화면 용도로 조회합니다.",
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    fun readHomeContent(
        @Parameter(description = "도메인 ID", example = "kr")
        domainId: String?,
    ): ResponseEntity<SiteHomeContentResponse>
}
