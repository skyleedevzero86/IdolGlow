package com.sleekydz86.idolglow.mypage.graphql

import com.sleekydz86.idolglow.mypage.application.dto.MypagePrimaryPromoResponse
import com.sleekydz86.idolglow.mypage.application.dto.MypagePromoStripResponse
import com.sleekydz86.idolglow.mypage.application.dto.MypageSecondaryPromoResponse

data class MypagePromoGraphQlResponse(
    val primary: MypagePrimaryPromoGraphQlResponse,
    val secondary: MypageSecondaryPromoGraphQlResponse,
) {
    companion object {
        fun from(response: MypagePromoStripResponse): MypagePromoGraphQlResponse =
            MypagePromoGraphQlResponse(
                primary = MypagePrimaryPromoGraphQlResponse.from(response.primary),
                secondary = MypageSecondaryPromoGraphQlResponse.from(response.secondary)
            )
    }
}

data class MypagePrimaryPromoGraphQlResponse(
    val variant: String,
    val textBeforeHighlight: String,
    val highlight: String,
    val textAfterHighlight: String,
    val href: String,
    val ctaLabel: String,
) {
    companion object {
        fun from(response: MypagePrimaryPromoResponse): MypagePrimaryPromoGraphQlResponse =
            MypagePrimaryPromoGraphQlResponse(
                variant = response.variant,
                textBeforeHighlight = response.textBeforeHighlight,
                highlight = response.highlight,
                textAfterHighlight = response.textAfterHighlight,
                href = response.href,
                ctaLabel = response.ctaLabel
            )
    }
}

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
                href = response.href
            )
    }
}
