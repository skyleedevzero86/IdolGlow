package com.sleekydz86.idolglow.payment.application.dto

data class TossBillingOverviewResponse(
    val method: TossBillingMethodResponse?,
    val methods: List<TossBillingMethodResponse>,
    val subscription: TossBillingSubscriptionResponse?,
    val charges: List<TossBillingChargeResponse>,
)
