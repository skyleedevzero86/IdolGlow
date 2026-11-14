package com.sleekydz86.idolglow.glowcommunity.application.dto

import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunitySort
import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunityTag

data class GlowCommunityPostSearchQuery(
    val tag: GlowCommunityTag? = null,
    val sort: GlowCommunitySort = GlowCommunitySort.LATEST,
    val keyword: String? = null,
    val page: Int = 0,
    val size: Int = DEFAULT_PAGE_SIZE,
) {
    companion object {
        const val DEFAULT_PAGE_SIZE = 20
    }
}

data class CreateGlowCommunityPostCommand(
    val userId: Long,
    val tag: GlowCommunityTag,
    val title: String,
    val content: String,
)

data class UpdateGlowCommunityPostCommand(
    val postId: Long,
    val userId: Long,
    val tag: GlowCommunityTag,
    val title: String,
    val content: String,
)
