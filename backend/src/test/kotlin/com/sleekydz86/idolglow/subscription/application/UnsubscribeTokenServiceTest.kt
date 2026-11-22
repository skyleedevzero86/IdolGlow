package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.subscription.application.port.out.ProductRecommendationMailSubscriptionPort
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailConsentSource
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailPreferredBasis
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailSubscription
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.time.LocalDateTime
import java.util.Base64

@DisplayName("추천 메일 수신 거부 토큰")
class UnsubscribeTokenServiceTest {
    private val port = mockk<ProductRecommendationMailSubscriptionPort>()
    private val service = UnsubscribeTokenService(port, SECRET)
    private val now = LocalDateTime.of(2026, 11, 1, 10, 0)

    @DisplayName("발급한 토큰은 검증되고 바꾼 토큰은 거절된다")
    @Test
    fun tokenRoundTripsAndRejectsTampered() {
        // given
        val subscription = subscription(id = 7L)
        every { port.findById(7L) } returns subscription
        every { port.findById(8L) } returns subscription(id = 8L)

        // when
        val token = service.issue(subscription)

        // then
        assertSame(subscription, service.verify(token))
        assertFalse(token.any { it == '=' || it == '+' || it == '/' })
        assertNull(service.verify(flipMiddleChar(token)))
        assertNull(service.verify(withSubscriptionId(token, "8")))
        assertNull(service.verify(withSubscriptionId(token, "+7")))
        assertNull(service.verify(""))
        assertNull(service.verify("not-a-token"))
        assertNull(service.verify("A".repeat(500)))
        assertNull(UnsubscribeTokenService(port, "other-secret").verify(token))
    }

    @DisplayName("거부했다가 다시 구독하면 예전 토큰은 거절되고 새 토큰만 통한다")
    @Test
    fun resubscribeInvalidatesOldToken() {
        // given
        val subscription = subscription(id = 7L)
        every { port.findById(7L) } returns subscription
        val oldToken = service.issue(subscription)

        // when
        subscription.unsubscribe(now.plusDays(1))

        // then
        assertSame(subscription, service.verify(oldToken))

        // when
        subscription.subscribe(ProductRecommendationMailConsentSource.MYPAGE, now.plusDays(2))
        val newToken = service.issue(subscription)

        // then
        assertNotEquals(oldToken, newToken)
        assertNull(service.verify(oldToken))
        assertSame(subscription, service.verify(newToken))
    }

    @DisplayName("비밀키가 없거나 비어 있으면 기동에 실패한다")
    @Test
    fun missingSecretFailsStartup() {
        // given
        val runner =
            ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration::class.java))
                .withBean(ProductRecommendationMailSubscriptionPort::class.java, { port })
                .withBean(UnsubscribeTokenService::class.java)

        // when / then
        runner.run { context -> assertNotNull(context.startupFailure) }
        runner
            .withPropertyValues("app.mail.recommendation.unsubscribe-secret=  ")
            .run { context -> assertNotNull(context.startupFailure) }
        runner
            .withPropertyValues("app.mail.recommendation.unsubscribe-secret=$SECRET")
            .run { context -> assertNull(context.startupFailure) }
    }

    private fun subscription(id: Long): ProductRecommendationMailSubscription =
        ProductRecommendationMailSubscription(
            id = id,
            userId = id + 100,
            active = true,
            preferredBasis = ProductRecommendationMailPreferredBasis.AUTO,
            consentedAt = now,
            consentSource = ProductRecommendationMailConsentSource.MY_SURVEY,
        )

    private fun flipMiddleChar(token: String): String {
        val index = token.length / 2
        val replacement = if (token[index] == 'A') 'B' else 'A'
        return token.substring(0, index) + replacement + token.substring(index + 1)
    }

    private fun withSubscriptionId(
        token: String,
        subscriptionId: String,
    ): String {
        val bytes = Base64.getUrlDecoder().decode(token)
        val separator = bytes.indexOf('.'.code.toByte())
        val replaced = subscriptionId.toByteArray() + bytes.copyOfRange(separator, bytes.size)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(replaced)
    }

    companion object {
        private const val SECRET = "test-recommendation-unsubscribe-secret"
    }
}
