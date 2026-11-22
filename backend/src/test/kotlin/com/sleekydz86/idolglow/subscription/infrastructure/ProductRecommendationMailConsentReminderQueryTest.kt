package com.sleekydz86.idolglow.subscription.infrastructure

import com.sleekydz86.idolglow.subscription.application.ProductRecommendationMailConsentReminderTxService
import com.sleekydz86.idolglow.subscription.application.port.out.ProductRecommendationMailSubscriptionPort
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailConsentSource
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailPreferredBasis
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailSubscription
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong

@DisplayName("상품 추천 메일 2년 수신 동의 안내 대상 조회")
class ProductRecommendationMailConsentReminderQueryTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var subscriptionPort: ProductRecommendationMailSubscriptionPort

    @DisplayName("동의일 또는 마지막 안내일이 2년 지난 활성 구독만 고른다")
    @Test
    fun consentReminderTargetsTwoYearOldConsents() {
        // given
        val now = LocalDateTime.of(2026, 11, 14, 10, 0)
        val threshold = ProductRecommendationMailConsentReminderTxService.dueThreshold(now)
        val neverReminded = save(consentedAt = now.minusYears(2).minusDays(1))
        val exactlyTwoYears = save(consentedAt = threshold)
        val remindedTwoYearsAgo =
            save(consentedAt = now.minusYears(5), consentRemindedAt = now.minusYears(2).minusDays(1))
        val remindedLastYear = save(consentedAt = now.minusYears(3), consentRemindedAt = now.minusYears(1))
        val notYetTwoYears = save(consentedAt = now.minusYears(2).plusDays(1))
        val inactive = save(consentedAt = now.minusYears(3), active = false)
        val resubscribed =
            save(consentedAt = now.minusYears(3), consentRemindedAt = now.minusYears(2).minusDays(3)).also {
                it.unsubscribe(now.minusYears(1))
                it.subscribe(ProductRecommendationMailConsentSource.MYPAGE, now.minusMonths(6))
                subscriptionPort.save(it)
            }
        val created =
            listOf(
                neverReminded,
                exactlyTwoYears,
                remindedTwoYearsAgo,
                remindedLastYear,
                notYetTwoYears,
                inactive,
                resubscribed,
            ).map { it.id }.toSet()

        // when
        val due =
            subscriptionPort
                .findAllConsentReminderDue(threshold)
                .map { it.id }
                .filter { it in created }

        // then
        assertEquals(listOf(neverReminded.id, exactlyTwoYears.id, remindedTwoYearsAgo.id), due)
    }

    private fun save(
        consentedAt: LocalDateTime,
        consentRemindedAt: LocalDateTime? = null,
        active: Boolean = true,
    ): ProductRecommendationMailSubscription =
        subscriptionPort.save(
            ProductRecommendationMailSubscription
                .create(
                    userId = nextUserId.incrementAndGet(),
                    consentSource = ProductRecommendationMailConsentSource.MY_SURVEY,
                    preferredBasis = ProductRecommendationMailPreferredBasis.AUTO,
                    now = consentedAt,
                ).also {
                    it.consentRemindedAt = consentRemindedAt
                    if (!active) it.unsubscribe(consentedAt.plusDays(1))
                },
        )

    companion object {
        private val nextUserId = AtomicLong(System.nanoTime() % 1_000_000_000L + 9_000_000_000L)
    }
}
