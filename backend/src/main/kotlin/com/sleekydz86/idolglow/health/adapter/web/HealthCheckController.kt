package com.sleekydz86.idolglow.health.adapter.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "헬스", description = "헬스 체크 API")
@RestController
class HealthCheckController(
    @param:Value("\${app.version:unknown}")
    private val appVersion: String,
) {
    @Operation(summary = "서버 상태 확인", description = "애플리케이션 기본 동작 여부를 확인합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "서버 정상 응답",
                content = [Content(schema = Schema(implementation = String::class, example = "정상"))],
            ),
        ],
    )
    @GetMapping("/health/check")
    fun healthCheck(): ResponseEntity<String> = ResponseEntity.ok("정상")

    @Operation(
        summary = "버전 포함 헬스",
        description = "배포 검증용으로 상태와 애플리케이션 버전을 JSON으로 반환합니다.",
    )
    @ApiResponse(responseCode = "200", description = "키: status, version, service")
    @GetMapping("/health")
    fun health(): Map<String, String> =
        mapOf(
            "status" to "UP",
            "version" to appVersion.trim().ifBlank { "unknown" },
            "service" to "idolglow-api",
        )
}
