package com.sleekydz86.idolglow.mypage.graphql

import com.sleekydz86.idolglow.global.resolver.AuthenticatedUserIdResolver
import com.sleekydz86.idolglow.mypage.application.MypagePromoService
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("isAuthenticated()")
class MyPageGraphQlController(
    private val mypagePromoService: MypagePromoService,
    private val authenticatedUserIdResolver: AuthenticatedUserIdResolver,
) {

    @QueryMapping
    fun myPromo(): MypagePromoGraphQlResponse =
        MypagePromoGraphQlResponse.from(
            mypagePromoService.promoStrip(authenticatedUserIdResolver.resolveRequired())
        )
}
