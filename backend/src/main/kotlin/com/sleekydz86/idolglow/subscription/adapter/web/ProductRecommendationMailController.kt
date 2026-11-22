package com.sleekydz86.idolglow.subscription.adapter.web

import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import com.sleekydz86.idolglow.subscription.adapter.web.request.SubscribeProductRecommendationMailRequest
import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailSettingResult
import com.sleekydz86.idolglow.subscription.application.port.`in`.ProductRecommendationMailUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "상품 추천 메일", description = "로그인 회원의 상품 추천 메일 수신 동의·설정·수신 거부 API")
@RestController
@RequestMapping("/me/recommendation-mail")
class ProductRecommendationMailController(
    private val productRecommendationMailUseCase: ProductRecommendationMailUseCase,
) {
    @Operation(summary = "내 상품 추천 메일 설정 조회")
    @GetMapping
    fun findMine(
        @LoginUser userId: Long,
    ): ResponseEntity<ProductRecommendationMailSettingResult> {
        val result = productRecommendationMailUseCase.findMine(userId)
        return ResponseEntity.ok(result)
    }

    @Operation(summary = "상품 추천 메일 수신 동의 또는 설정 변경")
    @PutMapping
    fun subscribe(
        @LoginUser userId: Long,
        @Valid @RequestBody request: SubscribeProductRecommendationMailRequest,
    ): ResponseEntity<ProductRecommendationMailSettingResult> {
        val result = productRecommendationMailUseCase.subscribe(request.toCommand(userId))
        return ResponseEntity.ok(result)
    }

    @Operation(summary = "상품 추천 메일 수신 거부")
    @DeleteMapping
    fun unsubscribe(
        @LoginUser userId: Long,
    ): ResponseEntity<ProductRecommendationMailSettingResult> {
        val result = productRecommendationMailUseCase.unsubscribeMine(userId)
        return ResponseEntity.ok(result)
    }
}
