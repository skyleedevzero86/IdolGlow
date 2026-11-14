package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.subscription.application.port.out.ProductRecommendationMailMemberPort
import com.sleekydz86.idolglow.subscription.application.port.out.ProductRecommendationMailSubscriptionPort
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailConsentSource
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailPreferredBasis
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailSubscription
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("상품 추천 메일 2년 수신 동의 안내 트랜잭션 서비스")
class ProductRecommendationMailConsentReminderTxServiceTest {
    private val subscriptionPort = mockk<ProductRecommendationMailSubscriptionPort>()
    private val memberPort = mockk<ProductRecommendationMailMemberPort>()
    private val tokenService = mockk<UnsubscribeTokenService>()
    private val txService =
        ProductRecommendationMailConsentReminderTxService(
            productRecommendationMailSubscriptionPort = subscriptionPort,
            productRecommendationMailMemberPort = memberPort,
            unsubscribeTokenService = tokenService,
        )
    private val now = LocalDateTime.of(2026, 11, 14, 10, 0)

    @DisplayName("2년 전 시각으로 조회하고 메일을 받을 수 없는 회원은 뺀다")
    @Test
    fun planQueriesTwoYearsAgoAndSkipsUnmailableMembers() {
        // given
        val mailable = subscription(id = 1L, userId = 11L)
        val unmailable = subscription(id = 2L, userId = 12L)
        every { subscriptionPort.findAllConsentReminderDue(now.minusYears(2)) } returns listOf(mailable, unmailable)
        every { memberPort.findMailableEmail(11L) } returns "fan@idolglow.test"
        every { memberPort.findMailableEmail(12L) } returns null
        every { tokenService.issue(mailable) } returns "token-1"

        // when
        val plans = txService.plan(now)

        // then
        val plan = plans.single()
        assertEquals(1L, plan.subscriptionId)
        assertEquals("fan@idolglow.test", plan.email)
        assertEquals(mailable.consentedAt, plan.consentedAt)
        assertEquals("token-1", plan.unsubscribeToken)
        verify(exactly = 0) { tokenService.issue(unmailable) }
    }

    @DisplayName("보내는 사이 수신 거부한 구독에는 안내 시각을 기록하지 않는다")
    @Test
    fun recordRemindedSkipsInactiveSubscription() {
        // given
        val unsubscribed = subscription(id = 3L, userId = 13L).also { it.unsubscribe(now.minusMinutes(1)) }
        every { subscriptionPort.findById(3L) } returns unsubscribed

        // when
        txService.recordReminded(3L, now)

        // then
        assertNull(unsubscribed.consentRemindedAt)
        verify(exactly = 0) { subscriptionPort.save(any()) }
    }

    @DisplayName("활성 구독에는 안내 시각을 기록한다")
    @Test
    fun recordRemindedStoresConsentRemindedAt() {
        // given
        val active = subscription(id = 4L, userId = 14L)
        every { subscriptionPort.findById(4L) } returns active
        every { subscriptionPort.save(active) } returns active

        // when
        txService.recordReminded(4L, now)

        // then
        assertEquals(now, active.consentRemindedAt)
        verify(exactly = 1) { subscriptionPort.save(active) }
    }

    private fun subscription(
        id: Long,
        userId: Long,
    ) = ProductRecommendationMailSubscription(
        id = id,
        userId = userId,
        active = true,
        preferredBasis = ProductRecommendationMailPreferredBasis.AUTO,
        consentedAt = now.minusYears(2).minusDays(1),
        consentSource = ProductRecommendationMailConsentSource.MY_SURVEY,
    )
}
