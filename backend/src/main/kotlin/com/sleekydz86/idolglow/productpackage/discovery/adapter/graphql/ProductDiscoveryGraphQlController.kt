package com.sleekydz86.idolglow.productpackage.discovery.graphql

import com.sleekydz86.idolglow.global.adapter.resolver.AuthenticatedUserIdResolver
import com.sleekydz86.idolglow.productpackage.discovery.application.ProductDiscoveryService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
class ProductDiscoveryGraphQlController(
    private val productDiscoveryService: ProductDiscoveryService,
    private val authenticatedUserIdResolver: AuthenticatedUserIdResolver,
) {

    @QueryMapping
    fun popularProducts(@Argument size: Int?): List<ProductRankingGraphQlResponse> =
        productDiscoveryService.findPopularProducts((size ?: 10).coerceIn(1, 50))
            .map(ProductRankingGraphQlResponse::from)

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    fun recommendedProducts(@Argument size: Int?): List<ProductRankingGraphQlResponse> =
        productDiscoveryService.findRecommendedProducts(
            authenticatedUserIdResolver.resolveRequired(),
            (size ?: 10).coerceIn(1, 50)
        ).map(ProductRankingGraphQlResponse::from)
}
