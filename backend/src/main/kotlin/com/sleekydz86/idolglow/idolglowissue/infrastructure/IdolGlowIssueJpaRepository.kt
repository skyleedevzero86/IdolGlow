package com.sleekydz86.idolglow.idolglowissue.infrastructure

import com.sleekydz86.idolglow.idolglowissue.domain.IdolGlowIssue
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface IdolGlowIssueJpaRepository : JpaRepository<IdolGlowIssue, Long> {
    fun findBySlug(slug: String): IdolGlowIssue?

    @Query("SELECT DISTINCT i FROM IdolGlowIssue i LEFT JOIN FETCH i.articles WHERE i.slug = :slug")
    fun findBySlugWithArticles(slug: String): IdolGlowIssue?

    @Query(
        "SELECT i FROM IdolGlowIssue i JOIN FETCH i.articles " +
            "WHERE i.issueDate <= :publishedOnOrBefore ORDER BY i.volume DESC, i.issueDate DESC",
    )
    fun findAllPublishedWithArticles(publishedOnOrBefore: LocalDate): List<IdolGlowIssue>

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

    fun findTopByOrderByVolumeDesc(): IdolGlowIssue?
}
