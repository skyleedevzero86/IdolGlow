package com.sleekydz86.idolglow.comment.infrastructure

import com.sleekydz86.idolglow.comment.application.port.out.ContentCommentAuthorPort
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component

@Component
class ContentCommentAuthorAdapter(
    private val entityManager: EntityManager,
) : ContentCommentAuthorPort {
    override fun findNicknames(userIds: Collection<Long>): Map<Long, String> {
        if (userIds.isEmpty()) return emptyMap()
        return entityManager
            .createQuery("select u.id, u.nickname.value from User u where u.id in :ids", Array<Any>::class.java)
            .setParameter("ids", userIds)
            .resultList
            .associate { row -> (row[0] as Number).toLong() to row[1].toString() }
    }

    override fun findAdminIds(userIds: Collection<Long>): Set<Long> {
        if (userIds.isEmpty()) return emptySet()
        return entityManager
            .createQuery("select u.id from User u where u.id in :ids and u.role = :role", java.lang.Long::class.java)
            .setParameter("ids", userIds)
            .setParameter("role", UserRole.ADMIN)
            .resultList
            .map { it.toLong() }
            .toSet()
    }
}
