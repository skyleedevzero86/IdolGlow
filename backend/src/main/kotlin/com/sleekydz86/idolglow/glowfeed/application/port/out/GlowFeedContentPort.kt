package com.sleekydz86.idolglow.glowfeed.application.port.out

import com.sleekydz86.idolglow.glowfeed.application.dto.GlowFeedContent
import com.sleekydz86.idolglow.glowfeed.domain.GlowFeedKind

interface GlowFeedContentPort {
    val kind: GlowFeedKind

    fun load(refIds: Collection<Long>): Map<Long, GlowFeedContent>
}
