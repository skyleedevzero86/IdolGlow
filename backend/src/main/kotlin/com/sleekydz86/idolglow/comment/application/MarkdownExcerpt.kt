package com.sleekydz86.idolglow.comment.application

object MarkdownExcerpt {
    private const val DEFAULT_LENGTH = 120
    private val IMAGE = Regex("!\\[[^\\]]*]\\([^)]*\\)")
    private val LINK = Regex("\\[([^\\]]*)]\\([^)]*\\)")
    private val SYMBOLS = Regex("[#*>`~_|]+")
    private val WHITESPACE = Regex("\\s+")

    fun of(
        markdown: String,
        length: Int = DEFAULT_LENGTH,
    ): String =
        markdown
            .replace(IMAGE, " [이미지] ")
            .replace(LINK, "$1")
            .replace(SYMBOLS, " ")
            .replace(WHITESPACE, " ")
            .trim()
            .take(length)
}
