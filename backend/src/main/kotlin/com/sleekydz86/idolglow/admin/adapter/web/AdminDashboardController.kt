package com.sleekydz86.idolglow.admin.adapter.web

import com.sleekydz86.idolglow.admin.application.AdminDashboardService
import com.sleekydz86.idolglow.admin.application.dto.AdminDashboardSummary
import com.sleekydz86.idolglow.admin.application.dto.AdminRecentMember
import com.sleekydz86.idolglow.servermetric.application.dto.ServerMetricTrend
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigInteger

@Tag(name = "관리자 대시보드", description = "관리자 마이페이지에 보여 줄 회원·방문·매출·서버 추이 집계 API")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/admin/dashboard", "/api/admin/dashboard")
class AdminDashboardController(
    private val adminDashboardService: AdminDashboardService,
) {
    @Operation(
        summary = "대시보드 요약",
        description =
            "전체 회원 수, 오늘·최근 7일 신규 가입, 오늘 방문자·신규 방문자와 최근 7일 추이, " +
                "이번 달 결제 순액·정기결제 월 환산액·월 매출 예상·활성 정기결제 수를 줍니다. 날짜는 서울 기준입니다.",
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/summary")
    fun summary(): AdminDashboardSummary = adminDashboardService.summarize()

    @Operation(
        summary = "최근 가입 회원",
        description = "가입 시각이 있는 회원을 최근 가입 순으로 줍니다. limit 는 1~20이고 범위 밖이면 끝값, 없거나 숫자가 아니면 8입니다.",
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/recent-members")
    fun recentMembers(
        @RequestParam(required = false) limit: String?,
    ): List<AdminRecentMember> = adminDashboardService.recentMembers(toIntWithinRange(limit))

    @Operation(
        summary = "서버 상태 추이",
        description =
            "5분마다 저장한 서버 점검 기록과 그 기간의 가용성·성공률·평균 응답시간을 줍니다. " +
                "hours 는 1~168이고 범위 밖이면 끝값, 없거나 숫자가 아니면 24입니다. HTTP 응답이 없으면 성공률은 비어 있습니다.",
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/server-metrics")
    fun serverMetrics(
        @RequestParam(required = false) hours: String?,
    ): ServerMetricTrend = adminDashboardService.serverMetrics(toIntWithinRange(hours))

    companion object {
        private val INT_MIN: BigInteger = BigInteger.valueOf(Int.MIN_VALUE.toLong())
        private val INT_MAX: BigInteger = BigInteger.valueOf(Int.MAX_VALUE.toLong())

        fun toIntWithinRange(raw: String?): Int? =
            raw
                ?.trim()
                ?.toBigIntegerOrNull()
                ?.coerceIn(INT_MIN, INT_MAX)
                ?.toInt()
    }
}
