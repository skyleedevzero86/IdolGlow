package com.sleekydz86.idolglow.glowcommunity.application.dto

data class GlowCommunityViewer(
    val userId: Long?,
    val admin: Boolean = false,
) {
    companion object {
        val ANONYMOUS = GlowCommunityViewer(userId = null, admin = false)

        fun member(userId: Long): GlowCommunityViewer = GlowCommunityViewer(userId = userId, admin = false)
    }
}
