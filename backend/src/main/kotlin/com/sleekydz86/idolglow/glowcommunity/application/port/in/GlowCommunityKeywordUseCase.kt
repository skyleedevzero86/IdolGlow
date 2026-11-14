package com.sleekydz86.idolglow.glowcommunity.application.port.`in`

import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityKeywordsResult

interface GlowCommunityKeywordUseCase {
    fun findKeywords(): GlowCommunityKeywordsResult
}
