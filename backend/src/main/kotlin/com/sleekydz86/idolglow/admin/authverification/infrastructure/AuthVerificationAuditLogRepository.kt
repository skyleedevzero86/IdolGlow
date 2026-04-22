package com.sleekydz86.idolglow.admin.authverification.infrastructure

import com.sleekydz86.idolglow.admin.authverification.domain.AuthVerificationAuditLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AuthVerificationAuditLogRepository : JpaRepository<AuthVerificationAuditLog, Long> {
    fun findByVerificationTypeOrderByCreatedAtDesc(verificationType: String, pageable: Pageable): Page<AuthVerificationAuditLog>

    @Query(
        """
        select a
        from AuthVerificationAuditLog a
        where (:keyword is null or :keyword = '' or lower(coalesce(a.email, '')) like lower(concat('%', :keyword, '%'))
            or lower(coalesce(a.username, '')) like lower(concat('%', :keyword, '%')))
        order by a.createdAt desc
        """,
    )
    fun searchByKeyword(
        @Param("keyword") keyword: String?,
        pageable: Pageable,
    ): Page<AuthVerificationAuditLog>

    @Query(
        """
        select a
        from AuthVerificationAuditLog a
        where a.verificationType = :verificationType
          and (:keyword is null or :keyword = '' or lower(coalesce(a.email, '')) like lower(concat('%', :keyword, '%'))
            or lower(coalesce(a.username, '')) like lower(concat('%', :keyword, '%')))
        order by a.createdAt desc
        """,
    )
    fun searchByTypeAndKeyword(
        @Param("verificationType") verificationType: String,
        @Param("keyword") keyword: String?,
        pageable: Pageable,
    ): Page<AuthVerificationAuditLog>
}
