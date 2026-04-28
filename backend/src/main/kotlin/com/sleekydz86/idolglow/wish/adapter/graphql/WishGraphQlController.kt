package com.sleekydz86.idolglow.wish.graphql

import com.sleekydz86.idolglow.global.graphql.toGraphQlIdLong
import com.sleekydz86.idolglow.global.adapter.resolver.AuthenticatedUserIdResolver
import com.sleekydz86.idolglow.wish.application.WishQueryService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("isAuthenticated()")
class WishGraphQlController(
    private val wishQueryService: WishQueryService,
    private val authenticatedUserIdResolver: AuthenticatedUserIdResolver,
) {

    @QueryMapping
    fun wishes(
        @Argument lastWishId: String?,
        @Argument size: Int?,
    ): List<WishProductGraphQlResponse> =
        wishQueryService.findWishedProductByNoOffset(
            userId = authenticatedUserIdResolver.resolveRequired(),
            lastWishId = lastWishId?.takeIf { it.isNotBlank() }?.toGraphQlIdLong("lastWishId"),
            size = (size ?: 20).coerceIn(1, 50)
        ).map(WishProductGraphQlResponse::from)
}
