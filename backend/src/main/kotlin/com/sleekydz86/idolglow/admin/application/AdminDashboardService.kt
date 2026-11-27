package com.sleekydz86.idolglow.admin.application

import com.sleekydz86.idolglow.admin.application.dto.AdminDashboardMembers
import com.sleekydz86.idolglow.admin.application.dto.AdminDashboardSummary
import com.sleekydz86.idolglow.admin.application.dto.AdminRecentMember
import com.sleekydz86.idolglow.global.support.DashboardTime
import com.sleekydz86.idolglow.payment.application.RevenueQueryService
import com.sleekydz86.idolglow.servermetric.application.ServerMetricQueryService
import com.sleekydz86.idolglow.servermetric.application.dto.ServerMetricTrend
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import com.sleekydz86.idolglow.visit.application.SiteVisitQueryService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminDashboardService(
    private val userRepository: UserRepository,
    private val siteVisitQueryService: SiteVisitQueryService,
    private val revenueQueryService: RevenueQueryService,
    private val serverMetricQueryService: ServerMetricQueryService,
    private val dashboardTime: DashboardTime,
) {
    fun summarize(): AdminDashboardSummary =
        AdminDashboardSummary(
            members = memberCounts(),
            visitors = siteVisitQueryService.summarize(),
            revenue = revenueQueryService.summarize(),
        )

    fun recentMembers(requestedLimit: Int?): List<AdminRecentMember> =
        userRepository
            .findRecentJoined(clampLimit(requestedLimit))
            .map { user ->
                AdminRecentMember(
                    userId = user.id,
                    nickname = user.nickname.value,
                    profileImageUrl = user.profileImageUrl,
                    joinedAt =
                        requireNotNull(user.joinedAt) {
                            "가입 시각이 없는 회원은 최근 가입 목록에 들어올 수 없습니다. userId=${user.id}"
                        },
                )
            }

    fun serverMetrics(requestedHours: Int?): ServerMetricTrend = serverMetricQueryService.trend(requestedHours)

    private fun memberCounts(): AdminDashboardMembers {
        val today = dashboardTime.today()
        val tomorrowStart = dashboardTime.startOf(today.plusDays(1))
        val weekStart = dashboardTime.startOf(today.minusDays(RECENT_DAYS - 1))
        return AdminDashboardMembers(
            total = userRepository.count(),
            joinedToday = userRepository.countJoinedBetween(dashboardTime.startOf(today), tomorrowStart),
            joinedLast7Days = userRepository.countJoinedBetween(weekStart, tomorrowStart),
        )
    }

    companion object {
        const val RECENT_DAYS: Long = 7L
        const val DEFAULT_LIMIT: Int = 8
        const val MIN_LIMIT: Int = 1
        const val MAX_LIMIT: Int = 20

        fun clampLimit(requested: Int?): Int = requested?.coerceIn(MIN_LIMIT, MAX_LIMIT) ?: DEFAULT_LIMIT
    }
}
