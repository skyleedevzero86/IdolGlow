package com.sleekydz86.idolglow.subway.domain

object SeoulMetroLineName {
    private val metroLine = Regex("(\\d+)\\s*호선")

    fun parseNumericLineId(lineNumLabel: String): String? {
        val m = metroLine.find(lineNumLabel) ?: return null
        val digits = m.groupValues[1]
        return digits.trimStart('0').ifEmpty { "0" }
    }
}
