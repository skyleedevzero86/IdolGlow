package com.sleekydz86.idolglow.glowfeed.application.port.out

import com.sleekydz86.idolglow.glowfeed.application.dto.GlowFeedViewTarget

interface GlowFeedViewerCountPort {
    fun countViewers(targets: Collection<GlowFeedViewTarget>): Map<GlowFeedViewTarget, Long>
}
