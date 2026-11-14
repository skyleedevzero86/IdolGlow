package com.sleekydz86.idolglow.idolglowissue.infrastructure

import com.sleekydz86.idolglow.idolglowissue.domain.IdolGlowIssue
import com.sleekydz86.idolglow.idolglowissue.domain.IdolGlowIssueRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class IdolGlowIssueRepositoryImpl(
    private val idolglowIssueJpaRepository: IdolGlowIssueJpaRepository,
) : IdolGlowIssueRepository {
    override fun findAllByLatest(): List<IdolGlowIssue> =
        idolglowIssueJpaRepository.findAll(
            Sort.by(
                Sort.Order.desc("volume"),
                Sort.Order.desc("issueDate"),
            ),
        )

    override fun findAllPublishedWithArticles(publishedOnOrBefore: LocalDate): List<IdolGlowIssue> =
        idolglowIssueJpaRepository.findAllPublishedWithArticles(publishedOnOrBefore)

    override fun findBySlug(slug: String): IdolGlowIssue? = idolglowIssueJpaRepository.findBySlug(slug)

    override fun findBySlugWithArticles(slug: String): IdolGlowIssue? = idolglowIssueJpaRepository.findBySlugWithArticles(slug)

    override fun existsByVolumeOrSlug(
        volume: Int,
        slug: String,
    ): Boolean = idolglowIssueJpaRepository.existsByVolumeOrSlug(volume, slug)

    override fun existsByVolumeAndIdNot(
        volume: Int,
        id: Long,
    ): Boolean = idolglowIssueJpaRepository.existsByVolumeAndIdNot(volume, id)

    override fun existsBySlugAndIdNot(
        slug: String,
        id: Long,
    ): Boolean = idolglowIssueJpaRepository.existsBySlugAndIdNot(slug, id)

    override fun findTopByLatestVolume(): IdolGlowIssue? = idolglowIssueJpaRepository.findTopByOrderByVolumeDesc()

    override fun save(issue: IdolGlowIssue): IdolGlowIssue = idolglowIssueJpaRepository.save(issue)

    override fun delete(issue: IdolGlowIssue) {
        idolglowIssueJpaRepository.delete(issue)
    }

    override fun count(): Long = idolglowIssueJpaRepository.count()
}
