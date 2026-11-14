package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.global.config.AppMailProperties
import com.sleekydz86.idolglow.global.config.AppPublicUrlProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("상품 추천 메일 2년 수신 동의 안내 메일 내용")
class ProductRecommendationMailConsentReminderComposerTest {
    private val composer =
        ProductRecommendationMailNoticeComposer(
            appMailProperties =
                AppMailProperties(
                    recommendation =
                        AppMailProperties.Recommendation(
                            subjectPrefix = "[IdolGlow]",
                            senderName = "아이돌글로우",
                            senderContact = "help@idolglow.test",
                        ),
                ),
            appPublicUrlProperties = AppPublicUrlProperties(frontendBaseUrl = "https://idolglow.test"),
        )
    private val unsubscribeUrl = "https://idolglow.test/recommendation-mail/unsubscribe?token=abc&v=1"

    @DisplayName("광고 표시 없이 동의 날짜, 유지 방법, 수신 거부 링크, 보내는 곳을 담는다")
    @Test
    fun consentReminderIsNotAdvertisingAndContainsConsentDateAndUnsubscribeLink() {
        // given
        val consentedAt = LocalDateTime.of(2024, 11, 13, 9, 30)

        // when
        val mail =
            composer.composeConsentReminder(
                recipient = "fan<1>@idolglow.test",
                consentedAt = consentedAt,
                unsubscribeUrl = unsubscribeUrl,
            )

        // then
        assertEquals("[IdolGlow] 상품 추천 메일 수신 동의 상태 안내", mail.subject)
        assertFalse(mail.subject.contains("(광고)"))
        assertTrue(mail.plainText.contains(unsubscribeUrl))
        assertTrue(mail.htmlBody.contains("https://idolglow.test/recommendation-mail/unsubscribe?token=abc&amp;v=1"))
        assertTrue(mail.htmlBody.contains("fan&lt;1&gt;@idolglow.test"))
        assertFalse(mail.htmlBody.contains("fan<1>"))
        listOf(mail.plainText, mail.htmlBody).forEach { body ->
            assertFalse(body.contains("(광고)"))
            assertTrue(body.contains("2024-11-13"))
            assertTrue(body.contains("계속 받으시려면 따로 하실 일이 없습니다."))
            assertTrue(body.contains("아이돌글로우"))
            assertTrue(body.contains("help@idolglow.test"))
        }
    }

    @DisplayName("보내는 곳 설정이 비어 있으면 발신자 이름과 주소를 쓴다")
    @Test
    fun consentReminderFallsBackToFromNameAndAddress() {
        // given
        val fallbackComposer =
            ProductRecommendationMailNoticeComposer(
                appMailProperties = AppMailProperties(fromName = "IdolGlow", fromAddress = "no-reply@idolglow.test"),
                appPublicUrlProperties = AppPublicUrlProperties(),
            )

        // when
        val mail =
            fallbackComposer.composeConsentReminder(
                recipient = "fan@idolglow.test",
                consentedAt = LocalDateTime.of(2024, 1, 2, 3, 4),
                unsubscribeUrl = unsubscribeUrl,
            )

        // then
        assertTrue(mail.plainText.contains("보내는 곳: IdolGlow / 연락처: no-reply@idolglow.test"))
    }
}
