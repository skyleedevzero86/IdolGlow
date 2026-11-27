package com.sleekydz86.idolglow.admin.application.dto

import com.sleekydz86.idolglow.payment.application.dto.RevenueSummary
import com.sleekydz86.idolglow.visit.application.dto.VisitorSummary
import java.time.LocalDateTime

data class AdminDashboardMembers(
    val total: Long,
    val joinedToday: Long,
    val joinedLast7Days: Long,
)

data class AdminDashboardSummary(
    val members: AdminDashboardMembers,
    val visitors: VisitorSummary,
    val revenue: RevenueSummary,
)

data class AdminRecentMember(
    val userId: Long,
    val nickname: String,
    val profileImageUrl: String?,
    val joinedAt: LocalDateTime,
)
