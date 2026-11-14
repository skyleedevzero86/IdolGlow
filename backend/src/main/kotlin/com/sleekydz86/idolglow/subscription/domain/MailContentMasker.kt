package com.sleekydz86.idolglow.subscription.domain

object MailContentMasker {
    private const val MASKED_TOKEN = "token=***"
    private val TOKEN_QUERY = Regex("""\btoken=[^&"'\s<>#]+""", RegexOption.IGNORE_CASE)

    fun maskTokens(text: String): String = TOKEN_QUERY.replace(text, MASKED_TOKEN)
}
