package com.sleekydz86.idolglow.mypage.graphql

import com.sleekydz86.idolglow.mypage.application.dto.MypagePrimaryPromoResponse
import com.sleekydz86.idolglow.mypage.application.dto.MypagePromoStripResponse
import com.sleekydz86.idolglow.mypage.application.dto.MypageSecondaryPromoResponse

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
