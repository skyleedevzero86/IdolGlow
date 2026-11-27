package com.sleekydz86.idolglow.glowcommunity.application

import com.sleekydz86.idolglow.glowcommunity.application.port.out.GlowCommunityReactionPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GlowCommunityMyReportService(
    private val glowCommunityReactionPort: GlowCommunityReactionPort,
) {
    fun countMine(userId: Long): Long = glowCommunityReactionPort.countReportsByUser(userId)
}
