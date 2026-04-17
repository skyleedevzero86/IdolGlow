package com.sleekydz86.idolglow.platform.user.infrastructure

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface UserPasswordHistoryJpaRepository : JpaRepository<UserPasswordHistoryEntity, Long> {
    fun findByUserIdOrderByCreatedAtDesc(userId: Long, pageable: Pageable): List<UserPasswordHistoryEntity>
}
