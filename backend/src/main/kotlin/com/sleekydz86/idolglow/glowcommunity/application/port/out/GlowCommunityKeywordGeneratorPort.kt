package com.sleekydz86.idolglow.glowcommunity.application.port.out

import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityKeywordSource

interface GlowCommunityKeywordGeneratorPort {
    fun generate(sources: List<GlowCommunityKeywordSource>): List<String>?
}
