package com.sleekydz86.idolglow.payment.adapter.graphql

import com.sleekydz86.idolglow.global.adapter.graphql.asGraphQlValue
import com.sleekydz86.idolglow.global.adapter.resolver.AuthenticatedUserIdResolver
import com.sleekydz86.idolglow.payment.application.AdminBillingStatsService
import com.sleekydz86.idolglow.payment.application.MyGlowPassStatsService
import com.sleekydz86.idolglow.payment.application.dto.AdminBillingStatsResponse
import com.sleekydz86.idolglow.payment.application.dto.BillingCycleCount
import com.sleekydz86.idolglow.payment.application.dto.MyGlowPassStatsResponse
import com.sleekydz86.idolglow.payment.application.dto.RenewalNoticeStats
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
class BillingStatsGraphQlController(
    private val adminBillingStatsService: AdminBillingStatsService,
    private val myGlowPassStatsService: MyGlowPassStatsService,
    private val authenticatedUserIdResolver: AuthenticatedUserIdResolver,
) {
    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun adminBillingStats(): AdminBillingStatsGraphQlResponse =
        AdminBillingStatsGraphQlResponse.from(adminBillingStatsService.stats())

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    fun myGlowPassStats(): MyGlowPassStatsGraphQlResponse =
        MyGlowPassStatsGraphQlResponse.from(myGlowPassStatsService.stats(authenticatedUserIdResolver.resolveRequired()))
}

data class AdminBillingStatsGraphQlResponse(
    val activeCount: Int,
    val cancelScheduledCount: Int,
    val pausedCount: Int,
    val retryingCount: Int,
    val byCycle: List<BillingCycleCountGraphQlResponse>,
    val monthlyRevenue: String,
    val renewalNotice: RenewalNoticeStatsGraphQlResponse,
) {
    companion object {
        fun from(response: AdminBillingStatsResponse) =
            AdminBillingStatsGraphQlResponse(
                activeCount = response.activeCount.toInt(),
                cancelScheduledCount = response.cancelScheduledCount.toInt(),
                pausedCount = response.pausedCount.toInt(),
                retryingCount = response.retryingCount.toInt(),
                byCycle = response.byCycle.map(BillingCycleCountGraphQlResponse::from),
                monthlyRevenue = response.monthlyRevenue.toString(),
                renewalNotice = RenewalNoticeStatsGraphQlResponse.from(response.renewalNotice),
            )
    }
}

data class BillingCycleCountGraphQlResponse(
    val cycleMonths: Int,
    val count: Int,
) {
    companion object {
        fun from(count: BillingCycleCount) = BillingCycleCountGraphQlResponse(count.cycleMonths, count.count.toInt())
    }
}

data class RenewalNoticeStatsGraphQlResponse(
    val sent: Int,
    val continued: Int,
    val stopped: Int,
    val noAnswer: Int,
) {
    companion object {
        fun from(stats: RenewalNoticeStats) =
            RenewalNoticeStatsGraphQlResponse(
                sent = stats.sent.toInt(),
                continued = stats.continued.toInt(),
                stopped = stats.stopped.toInt(),
                noAnswer = stats.noAnswer.toInt(),
            )
    }
}

data class MyGlowPassStatsGraphQlResponse(
    val membershipSince: String?,
    val totalPaid: String,
    val paidCount: Int,
    val savedByDiscount: String,
    val earlyBookings: Int,
    val freeCancels: Int,
) {
    companion object {
        fun from(response: MyGlowPassStatsResponse) =
            MyGlowPassStatsGraphQlResponse(
                membershipSince = response.membershipSince.asGraphQlValue(),
                totalPaid = response.totalPaid.toString(),
                paidCount = response.paidCount,
                savedByDiscount = response.savedByDiscount.toString(),
                earlyBookings = response.earlyBookings,
                freeCancels = response.freeCancels,
            )
    }
}
