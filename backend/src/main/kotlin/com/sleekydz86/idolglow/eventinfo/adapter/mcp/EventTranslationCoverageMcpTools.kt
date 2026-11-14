package com.sleekydz86.idolglow.eventinfo.adapter.mcp

import com.sleekydz86.idolglow.eventinfo.application.EventTranslationCoverageService
import com.sleekydz86.idolglow.eventinfo.domain.TranslationCoverage
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import org.springframework.ai.mcp.annotation.McpTool
import org.springframework.ai.mcp.annotation.McpToolParam
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["spring.ai.mcp.server.enabled"], havingValue = "true")
class EventTranslationCoverageMcpTools(
    private val eventTranslationCoverageService: EventTranslationCoverageService,
) {
    @McpTool(
        name = "translationCoverage",
        description =
            "운영 중인 공연의 언어별(en, ja, zh) 번역 현황을 센다. visible은 화면에 보이는 번역(PROVIDER·MANUAL), " +
                "machineOnly는 기계 번역만 있는 수, missing은 번역이 없는 수다.",
    )
    fun translationCoverage(
        @McpToolParam(description = "언어별로 돌려줄 번역 없는 공연 수. 비우면 20", required = false) sampleLimit: Int?,
    ): List<TranslationCoverage> {
        requireAdmin()
        return eventTranslationCoverageService.coverage(
            sampleLimit ?: EventTranslationCoverageService.DEFAULT_SAMPLE_LIMIT,
        )
    }

    private fun requireAdmin() {
        val roles =
            SecurityContextHolder
                .getContext()
                .authentication
                ?.authorities
                ?.mapNotNull { it.authority?.removePrefix("ROLE_") }
                .orEmpty()
        require(UserRole.ADMIN.name in roles) {
            "번역 현황 조회 권한이 없습니다. role=${roles.joinToString(",")}"
        }
    }
}
