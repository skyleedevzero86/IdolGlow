package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.global.config.AppMailProperties
import com.sleekydz86.idolglow.global.config.AppPublicUrlProperties
import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailUnsubscribeNotice
import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailUnsubscribeResult
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailMessage
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("상품 추천 메일 수신 거부 서비스")
class ProductRecommendationMailServiceTest {
    private val txService = mockk<ProductRecommendationMailTxService>()
    private val outboundMailPort = mockk<OutboundMailPort>(relaxed = true)
    private val mailProperties = AppMailProperties(enabled = true)
    private val service =
        ProductRecommendationMailService(
            productRecommendationMailTxService = txService,
            productRecommendationMailNoticeComposer =
                ProductRecommendationMailNoticeComposer(
                    mailProperties,
                    AppPublicUrlProperties("https://idolglow.test"),
                ),
            outboundMailPort = outboundMailPort,
            appMailProperties = mailProperties,
        )
    private val notice =
        ProductRecommendationMailUnsubscribeNotice(
            subscriptionId = 3L,
            email = "member@test.com",
            unsubscribedAt = LocalDateTime.of(2026, 11, 14, 10, 0),
        )

    @DisplayName("거부 트랜잭션이 끝난 뒤 광고 표시 없는 확인 메일을 한 통 보낸다")
    @Test
    fun sendsConfirmationAfterTransaction() {
        // given
        every { txService.unsubscribeByToken("token-1", any()) } returns notice
        val message = slot<OutboundMailMessage>()
        every { outboundMailPort.send(capture(message)) } returns Unit

        // when
        val result = service.unsubscribeByToken(" token-1 ")

        // then
        assertEquals(ProductRecommendationMailUnsubscribeResult.ACCEPTED, result)
        verifyOrder {
            txService.unsubscribeByToken("token-1", any())
            outboundMailPort.send(any())
        }
        assertEquals("member@test.com", message.captured.to)
        assertFalse(message.captured.subject.contains("(광고)"))
        assertTrue(message.captured.plainTextBody.contains("https://idolglow.test/mypage"))
        assertFalse(message.captured.plainTextBody.contains("token-1"))
    }

    @DisplayName("틀린 토큰이나 이미 거부된 구독이면 메일 없이 같은 결과를 돌려준다")
    @Test
    fun noMailWhenNothingChanged() {
        // given
        every { txService.unsubscribeByToken(any(), any()) } returns null

        // when
        val results = listOf("wrong", "  ", null).map(service::unsubscribeByToken)

        // then
        results.forEach { assertEquals(ProductRecommendationMailUnsubscribeResult.ACCEPTED, it) }
        verify(exactly = 1) { txService.unsubscribeByToken(any(), any()) }
        verify(exactly = 0) { outboundMailPort.send(any()) }
    }

    @DisplayName("확인 메일 발송이 실패해도 수신 거부 응답은 그대로 돌려준다")
    @Test
    fun mailFailureDoesNotFailUnsubscribe() {
        // given
        every { txService.unsubscribeByToken(any(), any()) } returns notice
        every { outboundMailPort.send(any()) } throws IllegalStateException("smtp down")

        // when
        val result = service.unsubscribeByToken("token-1")

        // then
        assertEquals(ProductRecommendationMailUnsubscribeResult.ACCEPTED, result)
        verify(exactly = 1) { outboundMailPort.send(any()) }
    }

    @DisplayName("메일 발송이 꺼져 있으면 확인 메일을 보내지 않는다")
    @Test
    fun skipsMailWhenDisabled() {
        // given
        every { txService.unsubscribeByToken(any(), any()) } returns notice
        val disabled =
            ProductRecommendationMailService(
                productRecommendationMailTxService = txService,
                productRecommendationMailNoticeComposer =
                    ProductRecommendationMailNoticeComposer(AppMailProperties(), AppPublicUrlProperties()),
                outboundMailPort = outboundMailPort,
                appMailProperties = AppMailProperties(enabled = false),
            )

        // when
        disabled.unsubscribeByToken("token-1")

        // then
        verify(exactly = 0) { outboundMailPort.send(any()) }
    }
}
