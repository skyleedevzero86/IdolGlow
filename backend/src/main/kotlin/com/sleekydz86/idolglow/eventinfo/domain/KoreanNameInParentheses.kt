package com.sleekydz86.idolglow.eventinfo.domain

object KoreanNameInParentheses {
    private val OPEN = setOf('(', '（')
    private val CLOSE = setOf(')', '）')
    private val HANGUL = Regex("[\\uAC00-\\uD7A3]")

    fun extract(title: String): String? = outerGroups(title)?.lastOrNull { HANGUL.containsMatchIn(it) }?.trim()

    private fun outerGroups(title: String): List<String>? {
        val groups = mutableListOf<String>()
        var depth = 0
        var start = -1
        title.forEachIndexed { index, char ->
            when (char) {
                in OPEN -> {
                    if (depth == 0) start = index + 1
                    depth += 1
                }
                in CLOSE -> {
                    if (depth == 0) return null
                    depth -= 1
                    if (depth == 0) groups += title.substring(start, index)
                }
            }
        }
        return if (depth == 0) groups else null
    }
}
