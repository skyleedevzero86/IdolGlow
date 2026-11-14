package com.sleekydz86.idolglow.subscription.domain

enum class MailSendKind(
    val label: String,
) {
    PRODUCT_RECOMMENDATION("상품 추천"),
    NEWSLETTER("뉴스레터"),
    WEBZINE("웹진"),
    UNSUBSCRIBE_CONFIRM("수신 거부 확인"),
    CONSENT_REMINDER("수신 동의 안내"),
    AUTH("인증"),
    PAYMENT("결제 안내"),
    OTHER("기타"),
    ;

    fun keepsBody(): Boolean = this != AUTH

    companion object {
        fun of(contentType: SubscriptionContentType): MailSendKind =
            when (contentType) {
                SubscriptionContentType.NEWSLETTER -> NEWSLETTER
                SubscriptionContentType.WEBZINE_ISSUE -> WEBZINE
                SubscriptionContentType.PRODUCT_RECOMMENDATION -> PRODUCT_RECOMMENDATION
            }
    }
}
