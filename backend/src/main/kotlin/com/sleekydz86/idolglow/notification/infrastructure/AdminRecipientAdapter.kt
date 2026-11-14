package com.sleekydz86.idolglow.notification.infrastructure

import com.sleekydz86.idolglow.notification.application.port.out.AdminRecipientPort
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component

@Component
class AdminRecipientAdapter(
    private val entityManager: EntityManager,
) : AdminRecipientPort {
    override fun findAdminUserIds(): List<Long> =
        entityManager
            .createQuery("select u.id from User u where u.role = :role order by u.id", java.lang.Long::class.java)
            .setParameter("role", UserRole.ADMIN)
            .resultList
            .map { it.toLong() }
}
