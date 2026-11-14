package com.sleekydz86.idolglow.glowfeed.application.port.out

import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetSlice
import com.sleekydz86.idolglow.glowfeed.application.dto.GlowFeedEntry
import com.sleekydz86.idolglow.glowfeed.domain.GlowFeedCategory
import java.time.LocalDate

interface GlowFeedIndexPort {
    fun findPage(
        category: GlowFeedCategory?,
        request: OffsetPageRequest,
        today: LocalDate,
    ): OffsetSlice<GlowFeedEntry>

    fun findLatest(
        category: GlowFeedCategory,
        today: LocalDate,
    ): GlowFeedEntry?

    fun findMostPopular(
        category: GlowFeedCategory,
        today: LocalDate,
    ): GlowFeedEntry?
}
