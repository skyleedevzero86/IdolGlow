package com.sleekydz86.idolglow.idolglowissue.domain

import java.time.LocalDate

interface IdolGlowIssueRepository {
    fun findAllByLatest(): List<IdolGlowIssue>

    fun findAllPublishedWithArticles(publishedOnOrBefore: LocalDate): List<IdolGlowIssue>

    fun findBySlug(slug: String): IdolGlowIssue?

    fun findBySlugWithArticles(slug: String): IdolGlowIssue?

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

    fun findTopByLatestVolume(): IdolGlowIssue?

    fun save(issue: IdolGlowIssue): IdolGlowIssue

    fun delete(issue: IdolGlowIssue)

    fun count(): Long
}
