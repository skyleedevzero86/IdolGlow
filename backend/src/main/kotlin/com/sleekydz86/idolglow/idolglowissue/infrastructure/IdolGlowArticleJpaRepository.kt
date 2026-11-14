package com.sleekydz86.idolglow.idolglowissue.infrastructure

import com.sleekydz86.idolglow.idolglowissue.domain.IdolGlowArticle
import org.springframework.data.jpa.repository.JpaRepository

interface IdolGlowArticleJpaRepository : JpaRepository<IdolGlowArticle, Long> {
    fun findByIssue_IdAndSlug(
        issueId: Long,
        slug: String,
    ): IdolGlowArticle?

    fun existsByIssue_IdAndSlug(
        issueId: Long,
        slug: String,
    ): Boolean
}
