package com.sleekydz86.idolglow.subscription.application.port.out

data class OutboundMailMessage(
    val to: String,
    val subject: String,
    val plainTextBody: String,
    val htmlBody: String,
    val headers: Map<String, String> = emptyMap(),
) {
    companion object {
        const val LIST_UNSUBSCRIBE = "List-Unsubscribe"
        const val LIST_UNSUBSCRIBE_POST = "List-Unsubscribe-Post"
        const val ONE_CLICK_POST_VALUE = "List-Unsubscribe=One-Click"
        private const val HTTPS_SCHEME = "https://"

        fun listUnsubscribeHeaders(oneClickUrl: String?): Map<String, String> {
            val url = oneClickUrl?.trim().orEmpty()
            if (!url.startsWith(HTTPS_SCHEME, ignoreCase = true)) return emptyMap()
            return mapOf(
                LIST_UNSUBSCRIBE to "<$url>",
                LIST_UNSUBSCRIBE_POST to ONE_CLICK_POST_VALUE,
            )
        }
    }
}
