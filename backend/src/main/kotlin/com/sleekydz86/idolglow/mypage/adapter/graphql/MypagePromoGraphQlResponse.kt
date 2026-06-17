package com.sleekydz86.idolglow.mypage.graphql

import com.sleekydz86.idolglow.mypage.application.dto.MypagePromoStripResponse

data class MypagePromoGraphQlResponse(
    val primary: MypagePrimaryPromoGraphQlResponse,
    val secondary: MypageSecondaryPromoGraphQlResponse,
) {
    companion object {
        fun from(response: MypagePromoStripResponse): MypagePromoGraphQlResponse =
            MypagePromoGraphQlResponse(
                primary = MypagePrimaryPromoGraphQlResponse.from(response.primary),
                secondary = MypageSecondaryPromoGraphQlResponse.from(response.secondary),
            )
    }
}
