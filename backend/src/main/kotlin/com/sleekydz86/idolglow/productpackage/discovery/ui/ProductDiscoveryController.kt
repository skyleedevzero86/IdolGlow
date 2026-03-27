package com.sleekydz86.idolglow.productpackage.discovery.ui

import com.sleekydz86.idolglow.global.resolver.LoginUser
import com.sleekydz86.idolglow.productpackage.discovery.application.ProductDiscoveryService
import com.sleekydz86.idolglow.productpackage.discovery.application.dto.ProductRankingResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/products")
class ProductDiscoveryController(
    private val productDiscoveryService: ProductDiscoveryService,
) {

    @GetMapping("/rankings/popular")
    fun findPopularProducts(
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<List<ProductRankingResponse>> =
        ResponseEntity.ok(productDiscoveryService.findPopularProducts(size.coerceIn(1, 50)))

    @GetMapping("/recommendations")
    fun findRecommendedProducts(
        @LoginUser userId: Long,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<List<ProductRankingResponse>> =
        ResponseEntity.ok(productDiscoveryService.findRecommendedProducts(userId, size.coerceIn(1, 50)))
}
