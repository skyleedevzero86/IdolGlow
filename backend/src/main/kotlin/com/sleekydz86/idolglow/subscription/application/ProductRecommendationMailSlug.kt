package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchFrequency
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields

object ProductRecommendationMailSlug {
    const val PREFIX = "product-reco-"

    fun of(
        frequency: SubscriptionDispatchFrequency,
        date: LocalDate,
    ): String =
        when (frequency) {
            SubscriptionDispatchFrequency.DAILY -> PREFIX + date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            SubscriptionDispatchFrequency.WEEKLY ->
                "%s%d-W%02d".format(
                    PREFIX,
                    date.get(IsoFields.WEEK_BASED_YEAR),
                    date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR),
                )
        }

    fun periodOf(slug: String): String = slug.removePrefix(PREFIX)
}
