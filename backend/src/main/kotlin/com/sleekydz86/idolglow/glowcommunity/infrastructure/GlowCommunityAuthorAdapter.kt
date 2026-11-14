package com.sleekydz86.idolglow.glowcommunity.infrastructure

import com.sleekydz86.idolglow.glowcommunity.application.port.out.GlowCommunityAuthorPort
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component

@Component
class GlowCommunityAuthorAdapter(
    private val entityManager: EntityManager,
) : GlowCommunityAuthorPort {
    override fun findNicknames(userIds: Collection<Long>): Map<Long, String> {
        if (userIds.isEmpty()) return emptyMap()
        return entityManager
            .createQuery("select u.id, u.nickname.value from User u where u.id in :ids", Array<Any>::class.java)
            .setParameter("ids", userIds)
            .resultList
            .associate { row -> (row[0] as Number).toLong() to row[1].toString() }
    }
}
