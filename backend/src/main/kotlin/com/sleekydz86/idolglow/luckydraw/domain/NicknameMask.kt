package com.sleekydz86.idolglow.luckydraw.domain

object NicknameMask {
    private const val LONG_VISIBLE = 4
    private const val SHORT_VISIBLE = 1
    private const val MASK = "***"

    fun mask(nickname: String): String {
        val codePoints = nickname.codePoints().toArray()
        val visible = if (codePoints.size > LONG_VISIBLE) LONG_VISIBLE else SHORT_VISIBLE
        return String(codePoints, 0, minOf(visible, codePoints.size)) + MASK
    }
}
