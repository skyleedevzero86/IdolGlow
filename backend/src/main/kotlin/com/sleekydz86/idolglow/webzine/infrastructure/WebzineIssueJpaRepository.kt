package com.sleekydz86.idolglow.webzine.infrastructure

import com.sleekydz86.idolglow.webzine.domain.WebzineIssue
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface WebzineIssueJpaRepository : JpaRepository<WebzineIssue, Long> {
    fun findBySlug(slug: String): WebzineIssue?

    @Query("SELECT DISTINCT i FROM WebzineIssue i LEFT JOIN FETCH i.articles WHERE i.slug = :slug")
    fun findBySlugWithArticles(slug: String): WebzineIssue?

    @Query(
        "SELECT i FROM WebzineIssue i JOIN FETCH i.articles " +
            "WHERE i.issueDate <= :publishedOnOrBefore ORDER BY i.volume DESC, i.issueDate DESC",
    )
    fun findAllPublishedWithArticles(publishedOnOrBefore: LocalDate): List<WebzineIssue>

    fun existsByVolumeOrSlug(
        volume: Int,
        slug: String,
    ): Boolean

    fun existsByVolumeAndIdNot(
        volume: Int,
        id: Long,
    ): Boolean

    fun existsBySlugAndIdNot(
        slug: String,
        id: Long,
    ): Boolean

    fun findTopByOrderByVolumeDesc(): WebzineIssue?
}
