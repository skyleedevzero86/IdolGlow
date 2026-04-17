package com.sleekydz86.idolglow.webzine.infrastructure

import com.sleekydz86.idolglow.webzine.domain.WebzineIssue
import com.sleekydz86.idolglow.webzine.domain.WebzineIssueRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

@Repository
class WebzineIssueRepositoryImpl(
    private val webzineIssueJpaRepository: WebzineIssueJpaRepository,
) : WebzineIssueRepository {

    override fun findAllByLatest(): List<WebzineIssue> =
        webzineIssueJpaRepository.findAll(
            Sort.by(
                Sort.Order.desc("volume"),
                Sort.Order.desc("issueDate"),
            )
        )

    override fun findBySlug(slug: String): WebzineIssue? =
        webzineIssueJpaRepository.findBySlug(slug)

    override fun existsByVolumeOrSlug(volume: Int, slug: String): Boolean =
        webzineIssueJpaRepository.existsByVolumeOrSlug(volume, slug)

    override fun existsByVolumeAndIdNot(volume: Int, id: Long): Boolean =
        webzineIssueJpaRepository.existsByVolumeAndIdNot(volume, id)

    override fun existsBySlugAndIdNot(slug: String, id: Long): Boolean =
        webzineIssueJpaRepository.existsBySlugAndIdNot(slug, id)

    override fun findTopByLatestVolume(): WebzineIssue? =
        webzineIssueJpaRepository.findTopByOrderByVolumeDesc()

    override fun save(issue: WebzineIssue): WebzineIssue =
        webzineIssueJpaRepository.save(issue)

    override fun delete(issue: WebzineIssue) {
        webzineIssueJpaRepository.delete(issue)
    }

    override fun count(): Long =
        webzineIssueJpaRepository.count()
}
