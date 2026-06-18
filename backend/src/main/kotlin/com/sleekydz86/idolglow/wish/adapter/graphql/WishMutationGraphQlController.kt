package com.sleekydz86.idolglow.wish.adapter.graphql

import com.sleekydz86.idolglow.global.adapter.graphql.toGraphQlIdLong
import com.sleekydz86.idolglow.global.adapter.resolver.AuthenticatedUserIdResolver
import com.sleekydz86.idolglow.wish.application.WishCommandService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("isAuthenticated()")
class WishMutationGraphQlController(
    private val authenticatedUserIdResolver: AuthenticatedUserIdResolver,
    private val wishCommandService: WishCommandService,
) {
    @MutationMapping
    fun toggleWish(
        @Argument productId: String,
    ): WishToggleGraphQlResponse {
        val userId = authenticatedUserIdResolver.resolveRequired()
        val response =
            wishCommandService.toggle(
                userId = userId,
                productId = productId.toGraphQlIdLong("productId"),
            )
        return WishToggleGraphQlResponse.from(response)
    }
}
