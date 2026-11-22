package com.sleekydz86.idolglow.subscription.adapter.web

import com.sleekydz86.idolglow.subscription.adapter.web.request.UnsubscribeProductRecommendationMailRequest
import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailUnsubscribePreviewResult
import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailUnsubscribeResult
import com.sleekydz86.idolglow.subscription.application.port.`in`.ProductRecommendationMailUnsubscribeUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "상품 추천 메일 수신 거부", description = "로그인 없이 메일 링크로 상품 추천 메일 수신을 거부하는 API")
@RestController
@RequestMapping("/recommendation-mail/unsubscribe")
class ProductRecommendationMailUnsubscribeController(
    private val productRecommendationMailUnsubscribeUseCase: ProductRecommendationMailUnsubscribeUseCase,
) {
    @Operation(summary = "수신 거부 링크 확인 (상태를 바꾸지 않음)")
    @GetMapping
    fun preview(
        @RequestParam(required = false) token: String?,
    ): ResponseEntity<ProductRecommendationMailUnsubscribePreviewResult> =
        ResponseEntity.ok(productRecommendationMailUnsubscribeUseCase.previewUnsubscribe(token))

    @Operation(summary = "수신 거부 처리")
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun unsubscribe(
        @RequestBody(required = false) request: UnsubscribeProductRecommendationMailRequest?,
    ): ResponseEntity<ProductRecommendationMailUnsubscribeResult> =
        ResponseEntity.ok(productRecommendationMailUnsubscribeUseCase.unsubscribeByToken(request?.token))

    @Operation(summary = "원클릭 수신 거부 (RFC 8058)")
    @PostMapping(
        "/one-click",
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE],
    )
    fun unsubscribeOneClick(
        @RequestParam(required = false) token: String?,
        @RequestParam(name = LIST_UNSUBSCRIBE_PARAM, required = false) listUnsubscribe: String?,
    ): ResponseEntity<ProductRecommendationMailUnsubscribeResult> {
        require(listUnsubscribe == ONE_CLICK_VALUE) { "List-Unsubscribe=One-Click 본문이 필요합니다." }
        return ResponseEntity.ok(productRecommendationMailUnsubscribeUseCase.unsubscribeByToken(token))
    }

    companion object {
        private const val LIST_UNSUBSCRIBE_PARAM = "List-Unsubscribe"
        private const val ONE_CLICK_VALUE = "One-Click"
    }
}
