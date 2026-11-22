package com.sleekydz86.idolglow.glowcommunity.application.port.out

interface GlowCommunityAuthorPort {
    fun findNicknames(userIds: Collection<Long>): Map<Long, String>
}
