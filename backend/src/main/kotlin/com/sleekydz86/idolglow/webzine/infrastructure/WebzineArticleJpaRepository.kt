package com.sleekydz86.idolglow.webzine.infrastructure

import com.sleekydz86.idolglow.webzine.domain.WebzineArticle
import org.springframework.data.jpa.repository.JpaRepository

interface WebzineArticleJpaRepository : JpaRepository<WebzineArticle, Long> {
    fun findByIssue_IdAndSlug(issueId: Long, slug: String): WebzineArticle?
    fun existsByIssue_IdAndSlug(issueId: Long, slug: String): Boolean
}
