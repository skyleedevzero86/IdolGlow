package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.global.config.AppMailProperties
import com.sleekydz86.idolglow.global.config.AppPublicUrlProperties
import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailConsentReminderPlan
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailMessage
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailPort
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDateTime

@DisplayName("상품 추천 메일 2년 수신 동의 안내 스케줄러")
class ProductRecommendationMailConsentReminderSchedulerTest {
    private val txService = mockk<ProductRecommendationMailConsentReminderTxService>()
    private val outboundMailPort = mockk<OutboundMailPort>()
    private val mailProperties = AppMailProperties(enabled = true)
    private val publicUrlProperties = AppPublicUrlProperties(frontendBaseUrl = "https://idolglow.test/")
    private val scheduler =
        ProductRecommendationMailConsentReminderScheduler(
            appMailProperties = mailProperties,
            appPublicUrlProperties = publicUrlProperties,
            productRecommendationMailConsentReminderTxService = txService,
            productRecommendationMailNoticeComposer =
                ProductRecommendationMailNoticeComposer(mailProperties, publicUrlProperties),
            outboundMailPort = outboundMailPort,
        ).also { it.clock = fixedClock(MORNING) }

    @DisplayName("대상에게 안내 메일을 보낸 뒤 consent_reminded_at 을 기록한다")
    @Test
    fun sendsReminderAndRecordsConsentRemindedAt() {
        // given
        every { txService.plan(MORNING) } returns listOf(plan(1L, "first@idolglow.test"))
        val messages = mutableListOf<OutboundMailMessage>()
        every { outboundMailPort.send(capture(messages)) } just runs
        every { txService.recordReminded(any(), any()) } just runs

        // when
        scheduler.remindDueConsents()

        // then
        verifyOrder {
            txService.plan(MORNING)
            outboundMailPort.send(any())
            txService.recordReminded(1L, MORNING)
        }
        val message = messages.single()
        assertEquals("first@idolglow.test", message.to)
        assertFalse(message.subject.contains("(광고)"))
        assertTrue(message.plainTextBody.contains("$UNSUBSCRIBE_PAGE?token=token-1"))
        assertTrue(message.headers.isEmpty())
    }

    @DisplayName("발송에 실패한 구독은 기록하지 않고 다음 구독을 계속 보낸다")
    @Test
    fun failedSendDoesNotRecordConsentReminded() {
        // given
        every { txService.plan(MORNING) } returns
            listOf(plan(1L, "broken@idolglow.test"), plan(2L, "ok@idolglow.test"))
        every { outboundMailPort.send(match { it.to == "broken@idolglow.test" }) } throws
            IllegalStateException("smtp down")
        every { outboundMailPort.send(match { it.to == "ok@idolglow.test" }) } just runs
        every { txService.recordReminded(any(), any()) } just runs

        // when
        scheduler.remindDueConsents()

        // then
        verify(exactly = 0) { txService.recordReminded(1L, any()) }
        verify(exactly = 1) { txService.recordReminded(2L, MORNING) }
    }

    @DisplayName("기록 저장이 실패해도 다음 구독을 계속 보낸다")
    @Test
    fun recordFailureDoesNotStopRemainingReminders() {
        // given
        every { txService.plan(MORNING) } returns
            listOf(plan(1L, "first@idolglow.test"), plan(2L, "second@idolglow.test"))
        every { outboundMailPort.send(any()) } just runs
        every { txService.recordReminded(1L, any()) } throws IllegalStateException("db down")
        every { txService.recordReminded(2L, any()) } just runs

        // when
        scheduler.remindDueConsents()

        // then
        verify(exactly = 2) { outboundMailPort.send(any()) }
        verify(exactly = 1) { txService.recordReminded(2L, MORNING) }
    }

    @DisplayName("메일 발송이 꺼져 있으면 대상을 찾지도 기록하지도 않는다")
    @Test
    fun skipsWhenMailDisabled() {
        // given
        mailProperties.enabled = false

        // when
        scheduler.remindDueConsents()

        // then
        verify(exactly = 0) { txService.plan(any()) }
        verify(exactly = 0) { outboundMailPort.send(any()) }
        verify(exactly = 0) { txService.recordReminded(any(), any()) }
    }

    @DisplayName("밤 9시부터 아침 8시 사이에는 보내지 않는다")
    @Test
    fun skipsAtNight() {
        // given
        scheduler.clock = fixedClock(LocalDateTime.of(2026, 11, 14, 21, 0))

        // when
        scheduler.remindDueConsents()

        // then
        verify(exactly = 0) { txService.plan(any()) }
        verify(exactly = 0) { outboundMailPort.send(any()) }
    }

    private fun plan(
        subscriptionId: Long,
        email: String,
    ) = ProductRecommendationMailConsentReminderPlan(
        subscriptionId = subscriptionId,
        email = email,
        consentedAt = LocalDateTime.of(2024, 11, 13, 9, 30),
        unsubscribeToken = "token-$subscriptionId",
    )

    companion object {
        private val MORNING = LocalDateTime.of(2026, 11, 14, 10, 0)
        private const val UNSUBSCRIBE_PAGE = "https://idolglow.test/recommendation-mail/unsubscribe"

        private fun fixedClock(at: LocalDateTime): Clock =
            Clock.fixed(at.atZone(IdolGlowDateTimes.ZONE).toInstant(), IdolGlowDateTimes.ZONE)
    }
}
