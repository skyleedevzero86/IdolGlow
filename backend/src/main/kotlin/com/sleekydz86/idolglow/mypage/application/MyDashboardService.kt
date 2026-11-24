package com.sleekydz86.idolglow.mypage.application

import com.sleekydz86.idolglow.glowcommunity.application.GlowCommunityMyReportService
import com.sleekydz86.idolglow.mypage.application.dto.MyDashboardResult
import com.sleekydz86.idolglow.visit.application.SiteVisitQueryService
import org.springframework.stereotype.Service

@Service
class MyDashboardService(
    private val glowCommunityMyReportService: GlowCommunityMyReportService,
    private val siteVisitQueryService: SiteVisitQueryService,
) {
    fun summarize(userId: Long): MyDashboardResult =
        MyDashboardResult(
            reportCount = glowCommunityMyReportService.countMine(userId),
            visitDays = siteVisitQueryService.countUserVisitDays(userId),
        )
}
