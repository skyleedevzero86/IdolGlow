package com.sleekydz86.idolglow.webzine.infrastructure

import com.sleekydz86.idolglow.webzine.domain.WebzineIssue
import org.springframework.data.jpa.repository.JpaRepository

interface WebzineIssueJpaRepository : JpaRepository<WebzineIssue, Long> {
    fun findBySlug(slug: String): WebzineIssue?
    fun existsByVolumeOrSlug(volume: Int, slug: String): Boolean
    fun existsByVolumeAndIdNot(volume: Int, id: Long): Boolean
    fun existsBySlugAndIdNot(slug: String, id: Long): Boolean
    fun findTopByOrderByVolumeDesc(): WebzineIssue?
}
