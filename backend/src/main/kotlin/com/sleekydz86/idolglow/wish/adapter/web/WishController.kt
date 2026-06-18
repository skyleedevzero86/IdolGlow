package com.sleekydz86.idolglow.wish.adapter.web

import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import com.sleekydz86.idolglow.wish.application.WishCommandService
import com.sleekydz86.idolglow.wish.application.WishQueryService
import com.sleekydz86.idolglow.wish.application.dto.WishToggleResponse
import com.sleekydz86.idolglow.wish.application.dto.WishedProductPagingResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/wishes")
@RestController
class WishController(
    private val wishCommandService: WishCommandService,
    private val wishQueryService: WishQueryService,
) : WishApi {
    @PostMapping("/{productId}")
    override fun toggleWish(
        @LoginUser userId: Long,
        @PathVariable productId: Long,
    ): ResponseEntity<WishToggleResponse> = ResponseEntity.ok(wishCommandService.toggle(userId, productId))

    @GetMapping
    override fun findWishes(
        @LoginUser userId: Long,
        @RequestParam(required = false) lastWishId: Long?,
        @RequestParam(required = false, defaultValue = "20") size: Int?,
    ): ResponseEntity<List<WishedProductPagingResponse>> {
        val resolvedSize = (size ?: 20).coerceIn(1, 50)

        return ResponseEntity.ok(
            wishQueryService.findWishedProductByNoOffset(
                userId,
                lastWishId,
                resolvedSize,
            ),
        )
    }
}
