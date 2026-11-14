package com.sleekydz86.idolglow.eventinfo.application.port.out

import com.sleekydz86.idolglow.eventinfo.domain.MissingTranslation

interface EventTranslationCoveragePort {
    fun countActive(): Long

    fun countVisible(locale: String): Long

    fun countMachineOnly(locale: String): Long

    fun countMissing(locale: String): Long

    fun findMissing(
        locale: String,
        limit: Int,
    ): List<MissingTranslation>
}
