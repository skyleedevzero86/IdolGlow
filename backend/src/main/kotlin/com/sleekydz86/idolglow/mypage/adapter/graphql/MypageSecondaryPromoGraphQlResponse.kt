package com.sleekydz86.idolglow.mypage.adapter.graphql

import com.sleekydz86.idolglow.mypage.application.dto.MypageSecondaryPromoResponse

data class MypageSecondaryPromoGraphQlResponse(
    val variant: String,
    val textBeforeStrong: String,
    val strong: String,
    val textAfterStrong: String,
    val metricValue: Int,
    val metricUnit: String,
    val href: String,
) {
    companion object {
        fun from(response: MypageSecondaryPromoResponse): MypageSecondaryPromoGraphQlResponse =
            MypageSecondaryPromoGraphQlResponse(
                variant = response.variant,
                textBeforeStrong = response.textBeforeStrong,
                strong = response.strong,
                textAfterStrong = response.textAfterStrong,
                metricValue = response.metricValue,
                metricUnit = response.metricUnit,
                href = response.href,
            )
    }
}
