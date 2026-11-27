package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.global.support.DashboardTime
import com.sleekydz86.idolglow.payment.application.dto.RevenueSummary
import com.sleekydz86.idolglow.payment.application.port.out.BillingAmount
import com.sleekydz86.idolglow.payment.application.port.out.RevenueQueryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.RoundingMode

@Service
@Transactional(readOnly = true)
class RevenueQueryService(
    private val revenueQueryPort: RevenueQueryPort,
    private val dashboardTime: DashboardTime,
) {
    fun summarize(): RevenueSummary {
        val monthStart = dashboardTime.startOfMonth()
        val nextMonthStart = dashboardTime.startOf(dashboardTime.today().withDayOfMonth(1).plusMonths(1))
        val thisMonthNet =
            revenueQueryPort
                .sumNetApprovedBetween(monthStart, nextMonthStart)
                .setScale(0, RoundingMode.FLOOR)
                .toLong()
        val active = revenueQueryPort.findActiveBillingAmounts()
        val billingMonthly = active.sumOf(::monthlyAmount)
        return RevenueSummary(
            thisMonthNetAmount = thisMonthNet,
            billingMonthlyAmount = billingMonthly,
            expectedMonthlyAmount = thisMonthNet + billingMonthly,
            activeBillingCount = active.size.toLong(),
        )
    }

    companion object {
        fun monthlyAmount(billing: BillingAmount): Long =
            Math.floorDiv(billing.amount, billing.cycleMonths.coerceAtLeast(1).toLong())
    }
}
