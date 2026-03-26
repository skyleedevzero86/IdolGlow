package com.sleekydz86.idolglow.wish.ui

import com.sleekydz86.idolglow.global.resolver.LoginUser
import com.sleekydz86.idolglow.wish.application.WishCommandService
import com.sleekydz86.idolglow.wish.application.WishQueryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RequestMapping("/wishes")
@RestController
class WishController(
    private val wishCommandService: WishCommandService,
    private val wishQueryService: WishQueryService
): WishApi {

    @PostMapping("/{productId}")
    override fun toggleWish(
        @LoginUser userId: Long,
        @PathVariable productId: Long
    ): ResponseEntity<WishToggleResponse> {
        return ResponseEntity.ok(wishCommandService.toggle(userId, productId))
    }

    @GetMapping
    override fun findWishes(
        @LoginUser userId: Long,
        @RequestParam(required = false) lastWishId: Long?,
        @RequestParam(required = false, defaultValue = "20") size: Int?
    ): ResponseEntity<List<WishedProductPagingResponse>> {
        val resolvedSize = (size ?: 20).coerceIn(1, 50)

        return ResponseEntity.ok(
            wishQueryService.findWishedProductByNoOffset(
                userId,
                lastWishId,
                resolvedSize
            )
        )
    }
}
