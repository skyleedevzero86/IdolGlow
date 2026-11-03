package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.domain.RenewalDecision
import com.sleekydz86.idolglow.payment.domain.TossBillingMethod
import com.sleekydz86.idolglow.payment.domain.TossBillingRenewalNotice
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscription
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscriptionStatus
import com.sleekydz86.idolglow.payment.infrastructure.BillingKeyCrypto
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingRenewalNoticeJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingSubscriptionJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.Optional

@DisplayName("정기결제 갱신 안내")
class BillingRenewalNoticeTxServiceTest {
    private val subscriptionRepository = mockk<TossBillingSubscriptionJpaRepository>()
    private val noticeRepository = mockk<TossBillingRenewalNoticeJpaRepository>()
    private val service = BillingRenewalNoticeTxService(subscriptionRepository, noticeRepository)
    private val periodEnd = LocalDateTime.of(2027, 2, 3, 9, 0)
    private val now = LocalDateTime.of(2027, 1, 27, 9, 0)

    @DisplayName("안내를 만들면 원문 토큰은 메일용으로만 돌려주고 DB에는 해시만 저장한다")
    @Test
    fun storesOnlyTokenHash() {
        // given
        val subscription = subscription()
        val saved = slot<TossBillingRenewalNotice>()
        every { subscriptionRepository.findById(10L) } returns Optional.of(subscription)
        every { noticeRepository.existsBySubscriptionIdAndPeriodEnd(10L, periodEnd) } returns false
        every { noticeRepository.saveAndFlush(capture(saved)) } answers { saved.captured }

        // when
        val prepared = service.prepare(10L)

        // then
        assertEquals(periodEnd, prepared?.periodEnd)
        assertNotEquals(prepared?.token, saved.captured.tokenHash)
        assertEquals(BillingKeyCrypto.sha256Hex(prepared!!.token), saved.captured.tokenHash)
    }

    @DisplayName("같은 이용 기간의 안내가 이미 있으면 다시 만들지 않는다")
    @Test
    fun skipsWhenAlreadyNoticed() {
        // given
        every { subscriptionRepository.findById(10L) } returns Optional.of(subscription())
        every { noticeRepository.existsBySubscriptionIdAndPeriodEnd(10L, periodEnd) } returns true

        // when
        val prepared = service.prepare(10L)

        // then
        assertNull(prepared)
    }

    @DisplayName("이번 기간만 이용을 고르면 이용 기간 끝에 종료되도록 해지 예약된다")
    @Test
    fun stopSchedulesCancelAtPeriodEnd() {
        // given
        val subscription = subscription()
        givenNotice(subscription)

        // when
        val response = service.decide("token-1", RenewalDecision.STOP, now)

        // then
        assertEquals(TossBillingSubscriptionStatus.CANCEL_SCHEDULED, subscription.status)
        assertEquals(periodEnd, subscription.endsAt)
        assertFalse(subscription.autoRenew)
        assertEquals(RenewalDecision.STOP, response.decision)
    }

    @DisplayName("이번 기간만 이용하던 구독도 계속 이용을 고르면 다시 자동 연장된다")
    @Test
    fun continueWithdrawsScheduledCancel() {
        // given
        val subscription = subscription(autoRenew = false)
        subscription.scheduleCancel(now)
        givenNotice(subscription)

        // when
        service.decide("token-1", RenewalDecision.CONTINUE, now)

        // then
        assertEquals(TossBillingSubscriptionStatus.ACTIVE, subscription.status)
        assertTrue(subscription.autoRenew)
        assertNull(subscription.endsAt)
    }

    @DisplayName("이용 기간이 끝난 안내 링크로는 선택을 바꿀 수 없다")
    @Test
    fun rejectsAfterPeriodEnd() {
        // given
        val subscription = subscription()
        givenNotice(subscription)

        // when
        val error =
            assertThrows<IllegalArgumentException> {
                service.decide("token-1", RenewalDecision.STOP, periodEnd)
            }

        // then
        assertTrue(error.message!!.startsWith("이용 기간이 이미 끝나"))
        assertEquals(TossBillingSubscriptionStatus.ACTIVE, subscription.status)
    }

    @DisplayName("이미 다음 기간으로 넘어간 구독의 옛 안내 링크는 거절한다")
    @Test
    fun rejectsStaleNotice() {
        // given
        val subscription = subscription()
        givenNotice(subscription)
        subscription.nextBillingAt = periodEnd.plusMonths(3)

        // when
        val error =
            assertThrows<IllegalArgumentException> {
                service.decide("token-1", RenewalDecision.STOP, now)
            }

        // then
        assertTrue(error.message!!.startsWith("이미 다음 이용 기간으로 넘어간 안내"))
    }

    @DisplayName("올바르지 않은 토큰은 거절한다")
    @Test
    fun rejectsUnknownToken() {
        // given
        every { noticeRepository.findByTokenHash(any()) } returns null

        // when
        val error = assertThrows<IllegalArgumentException> { service.preview("wrong", now) }

        // then
        assertEquals("안내 링크가 올바르지 않습니다.", error.message)
    }

    private fun givenNotice(subscription: TossBillingSubscription) {
        val notice =
            TossBillingRenewalNotice(
                subscriptionId = 10L,
                userId = 1L,
                periodEnd = periodEnd,
                tokenHash = BillingKeyCrypto.sha256Hex("token-1"),
            )
        every { noticeRepository.findByTokenHash(BillingKeyCrypto.sha256Hex("token-1")) } returns notice
        every { subscriptionRepository.findById(10L) } returns Optional.of(subscription)
    }

    private fun subscription(autoRenew: Boolean = true): TossBillingSubscription {
        val method =
            TossBillingMethod(
                userId = 1L,
                customerKey = "idolglow-user-1",
                billingKey = "billing-key",
            )
        return TossBillingSubscription(
            id = 10L,
            userId = 1L,
            billingMethod = method,
            orderName = "IdolGlow Glow Pass 3개월",
            amount = 13_900L,
            nextBillingAt = periodEnd,
            cycleMonths = 3,
            autoRenew = autoRenew,
        )
    }
}
