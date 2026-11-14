package com.sleekydz86.idolglow.subscription.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("상품 추천 메일 구독")
class ProductRecommendationMailSubscriptionTest {
    private val consentedAt = LocalDateTime.of(2026, 11, 1, 10, 0)

    @DisplayName("처음 구독하면 활성이고 토큰 버전은 1이다")
    @Test
    fun createStartsActiveWithFirstTokenVersion() {
        val subscription = newSubscription()

        assertTrue(subscription.active)
        assertEquals(1, subscription.tokenVersion)
        assertEquals(consentedAt, subscription.consentedAt)
        assertEquals(ProductRecommendationMailConsentSource.MY_SURVEY, subscription.consentSource)
        assertEquals(ProductRecommendationMailPreferredBasis.AUTO, subscription.preferredBasis)
        assertNull(subscription.unsubscribedAt)
    }

    @DisplayName("회원 ID가 없으면 만들 수 없다")
    @Test
    fun createRejectsMissingUser() {
        assertThrows(IllegalArgumentException::class.java) {
            ProductRecommendationMailSubscription.create(
                userId = 0L,
                consentSource = ProductRecommendationMailConsentSource.MYPAGE,
                preferredBasis = ProductRecommendationMailPreferredBasis.AUTO,
                now = consentedAt,
            )
        }
    }

    @DisplayName("구독 중에 다시 구독해도 동의 시각과 토큰 버전이 바뀌지 않는다")
    @Test
    fun subscribeWhileActiveChangesNothing() {
        val subscription = newSubscription()

        subscription.subscribe(ProductRecommendationMailConsentSource.MYPAGE, consentedAt.plusDays(3))

        assertTrue(subscription.active)
        assertEquals(1, subscription.tokenVersion)
        assertEquals(consentedAt, subscription.consentedAt)
        assertEquals(ProductRecommendationMailConsentSource.MY_SURVEY, subscription.consentSource)
    }

    @DisplayName("수신 거부하면 비활성이 되고 거부 시각이 남는다")
    @Test
    fun unsubscribeDeactivates() {
        val subscription = newSubscription()
        val unsubscribedAt = consentedAt.plusDays(1)

        subscription.unsubscribe(unsubscribedAt)

        assertFalse(subscription.active)
        assertEquals(unsubscribedAt, subscription.unsubscribedAt)
    }

    @DisplayName("이미 거부한 구독을 다시 거부해도 처음 거부 시각이 그대로다")
    @Test
    fun unsubscribeTwiceKeepsFirstTime() {
        val subscription = newSubscription()
        val first = consentedAt.plusDays(1)

        subscription.unsubscribe(first)
        subscription.unsubscribe(first.plusDays(1))

        assertFalse(subscription.active)
        assertEquals(first, subscription.unsubscribedAt)
        assertEquals(1, subscription.tokenVersion)
    }

    @DisplayName("거부했다가 다시 구독하면 토큰 버전이 오르고 동의 시각과 화면이 새로 남는다")
    @Test
    fun resubscribeAfterUnsubscribeBumpsTokenVersion() {
        val subscription = newSubscription()
        subscription.consentRemindedAt = consentedAt.plusYears(2)
        subscription.unsubscribe(consentedAt.plusYears(2).plusDays(1))
        val resubscribedAt = consentedAt.plusYears(2).plusDays(5)

        subscription.subscribe(ProductRecommendationMailConsentSource.MYPAGE, resubscribedAt)

        assertTrue(subscription.active)
        assertEquals(2, subscription.tokenVersion)
        assertEquals(resubscribedAt, subscription.consentedAt)
        assertEquals(ProductRecommendationMailConsentSource.MYPAGE, subscription.consentSource)
        assertNull(subscription.unsubscribedAt)
        assertNull(subscription.consentRemindedAt)
    }

    @DisplayName("추천 기준을 인기순으로 바꿀 수 있다")
    @Test
    fun changeBasis() {
        val subscription = newSubscription()

        subscription.changeBasis(ProductRecommendationMailPreferredBasis.POPULAR)

        assertEquals(ProductRecommendationMailPreferredBasis.POPULAR, subscription.preferredBasis)
    }

    private fun newSubscription(): ProductRecommendationMailSubscription =
        ProductRecommendationMailSubscription.create(
            userId = 10L,
            consentSource = ProductRecommendationMailConsentSource.MY_SURVEY,
            preferredBasis = ProductRecommendationMailPreferredBasis.AUTO,
            now = consentedAt,
        )
}
