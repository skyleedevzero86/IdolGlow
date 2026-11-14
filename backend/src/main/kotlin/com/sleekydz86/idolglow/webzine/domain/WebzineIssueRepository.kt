package com.sleekydz86.idolglow.webzine.domain

import java.time.LocalDate

interface WebzineIssueRepository {
    fun findAllByLatest(): List<WebzineIssue>

    fun findAllPublishedWithArticles(publishedOnOrBefore: LocalDate): List<WebzineIssue>

    fun findBySlug(slug: String): WebzineIssue?

    fun findBySlugWithArticles(slug: String): WebzineIssue?

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

    fun findTopByLatestVolume(): WebzineIssue?

    fun save(issue: WebzineIssue): WebzineIssue

    fun delete(issue: WebzineIssue)

    fun count(): Long
}
