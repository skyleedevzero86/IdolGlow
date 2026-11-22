package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.global.config.AppMailProperties
import com.sleekydz86.idolglow.global.config.AppPublicUrlProperties
import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailBasis
import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("상품 추천 메일 내용")
class ProductRecommendationMailComposerTest {
    private val composer =
        SubscriptionEmailComposer(
            appMailProperties =
                AppMailProperties(
                    recommendation =
                        AppMailProperties.Recommendation(
                            subjectPrefix = "[IdolGlow]",
                            senderName = "아이돌글로우",
                            senderContact = "help@idolglow.test",
                        ),
                ),
            appPublicUrlProperties =
                AppPublicUrlProperties(
                    publicBaseUrl = "https://api.idolglow.test",
                    frontendBaseUrl = "https://idolglow.test",
                ),
        )
    private val unsubscribeUrl = "https://idolglow.test/recommendation-mail/unsubscribe?token=abc"

    @DisplayName("제목은 취향·인기 모두 (광고) 표시로 시작한다")
    @Test
    fun subjectStartsWithAdvertisingMark() {
        // given
        val items = listOf(item(1L, "헤어 메이크업"))

        // when
        val personalized = compose(ProductRecommendationMailBasis.PERSONALIZED, items)
        val popular = compose(ProductRecommendationMailBasis.POPULAR, items)

        // then
        assertEquals("(광고) [IdolGlow] 내 취향 추천 상품", personalized.subject)
        assertEquals("(광고) [IdolGlow] 이번 주 인기 상품", popular.subject)
        assertTrue(personalized.plainText.startsWith("(광고) "))
        assertTrue(popular.htmlBody.contains("(광고)"))
    }

    @DisplayName("본문에 수신 거부 링크, 보내는 곳 이름·연락처, 가격 안내, 설문 수정 링크가 들어간다")
    @Test
    fun bodyContainsUnsubscribeLinkAndSenderInfo() {
        // given
        val items =
            listOf(
                item(
                    id = 7L,
                    name = "<b>청순 화보</b>",
                    reasons = listOf("선호 컨셉 '러블리/청순' 태그", "방문 장소 '성수'", "방문 기간 3일 중 2일 예약 가능"),
                    highlights = listOf("선호 컨셉 '러블리/청순' 태그", "방문 장소 '성수'"),
                ),
            )

        // when
        val mail =
            composer.composeProductRecommendation(
                recipient = "fan@idolglow.test",
                basis = ProductRecommendationMailBasis.PERSONALIZED,
                items = items,
                unsubscribeUrl = unsubscribeUrl,
            )

        // then
        listOf(mail.plainText, mail.htmlBody).forEach { body ->
            assertTrue(body.contains(unsubscribeUrl))
            assertTrue(body.contains("아이돌글로우"))
            assertTrue(body.contains("help@idolglow.test"))
            assertTrue(body.contains("가격은 발송 시점 기준이며, 예약 가능 여부는 상세 화면에서 확인해 주세요."))
            assertTrue(body.contains("https://idolglow.test/my-survey"))
            assertTrue(body.contains("https://idolglow.test/products/7"))
            assertTrue(body.contains("12,000원"))
        }
        assertTrue(mail.plainText.contains("추천 기준: 내 설문 취향 (선호 컨셉 '러블리/청순' 태그, 방문 장소 '성수')"))
        assertFalse(mail.plainText.contains("방문 기간 3일 중 2일 예약 가능"))
        assertTrue(mail.htmlBody.contains("&lt;b&gt;청순 화보&lt;/b&gt;"))
        assertFalse(mail.htmlBody.contains("<b>청순 화보</b>"))
        assertTrue(mail.htmlBody.contains("&#39;러블리/청순&#39;"))
    }

    @DisplayName("인기순은 기준을 인기순으로, 설문 대체면 대체 안내를 쓰고 최대 5개만 넣는다")
    @Test
    fun popularBasisLineAndFallbackNotice() {
        // given
        val items = (1L..8L).map { item(it, "상품$it") }

        // when
        val mail =
            composer.composeProductRecommendation(
                recipient = "fan@idolglow.test",
                basis = ProductRecommendationMailBasis.POPULAR,
                items = items,
                unsubscribeUrl = unsubscribeUrl,
                surveyFallback = true,
            )

        // then
        assertTrue(mail.plainText.contains("추천 기준: 인기순"))
        assertTrue(mail.plainText.contains("설문과 맞는 상품이 없어 인기 상품을 보내 드려요."))
        assertTrue(mail.plainText.contains("https://idolglow.test/products/5"))
        assertFalse(mail.plainText.contains("https://idolglow.test/products/6"))
    }

    private fun compose(
        basis: ProductRecommendationMailBasis,
        items: List<ProductRecommendationMailItem>,
    ) = composer.composeProductRecommendation("a@idolglow.test", basis, items, unsubscribeUrl)

    private fun item(
        id: Long,
        name: String,
        reasons: List<String> = emptyList(),
        highlights: List<String> = emptyList(),
    ): ProductRecommendationMailItem =
        ProductRecommendationMailItem(
            productId = id,
            name = name,
            minPrice = BigDecimal("12000.00"),
            reasons = reasons,
            highlights = highlights,
        )
}
