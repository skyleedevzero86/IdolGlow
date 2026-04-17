package com.sleekydz86.idolglow.global.ui

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@Tag(
    name = "서비스 루트",
    description = "서비스 메타·브라우저 기본 요청용 엔드포인트(인증 없음).",
)
@RestController
class RootController {

    @SecurityRequirements
    @Operation(
        summary = "API 안내",
        description = "서비스 이름과 헬스체크·Swagger·OpenAPI 문서 경로를 JSON으로 반환합니다.",
    )
    @ApiResponse(responseCode = "200", description = "키: service, health, swaggerUi, openApi")
    @GetMapping("/")
    fun root(): Map<String, String> =
        mapOf(
            "service" to "idolglow-api",
            "health" to "/health/check",
            "swaggerUi" to "/swagger-ui.html",
            "openApi" to "/v3/api-docs",
        )

    @SecurityRequirements
    @Operation(
        summary = "파비콘",
        description = "브라우저가 자동으로 요청하는 `/favicon.ico`에 대해 본문 없이 204를 반환합니다.",
    )
    @ApiResponse(responseCode = "204", description = "콘텐츠 없음")
    @GetMapping("/favicon.ico")
    fun favicon(): ResponseEntity<Void> =
        ResponseEntity.status(HttpStatus.NO_CONTENT).build()
}
