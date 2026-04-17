package com.sleekydz86.idolglow.health.ui

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "헬스", description = "헬스 체크 API")
@RestController
class HealthCheckController {
    @Operation(summary = "서버 상태 확인", description = "애플리케이션 기본 동작 여부를 확인합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "서버 정상 응답",
                content = [Content(schema = Schema(implementation = String::class, example = "up"))]
            )
        ]
    )
    @GetMapping("/health/check")
    fun healthCheck(): ResponseEntity<String> {
        return ResponseEntity.ok("up")
    }
}
