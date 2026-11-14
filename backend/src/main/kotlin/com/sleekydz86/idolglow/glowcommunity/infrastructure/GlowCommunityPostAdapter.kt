package com.sleekydz86.idolglow.glowcommunity.infrastructure

import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetSlice
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityPostSearchQuery
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityViewer
import com.sleekydz86.idolglow.glowcommunity.application.port.out.GlowCommunityPostPort
import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunityPost
import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunitySort
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component

@Component
class GlowCommunityPostAdapter(
    private val glowCommunityPostJpaRepository: GlowCommunityPostJpaRepository,
) : GlowCommunityPostPort {
    override fun save(post: GlowCommunityPost): GlowCommunityPost = glowCommunityPostJpaRepository.save(post)

    override fun findActiveById(postId: Long): GlowCommunityPost? =
        glowCommunityPostJpaRepository.findActiveById(postId)

    override fun findActiveByIdForUpdate(postId: Long): GlowCommunityPost? =
        glowCommunityPostJpaRepository.findActiveByIdForUpdate(postId)

    override fun findAllByIds(postIds: Collection<Long>): List<GlowCommunityPost> =
        if (postIds.isEmpty()) emptyList() else glowCommunityPostJpaRepository.findAllById(postIds)

    override fun search(
        query: GlowCommunityPostSearchQuery,
        viewer: GlowCommunityViewer,
    ): OffsetSlice<GlowCommunityPost> {
        val sort =
            when (query.sort) {
                GlowCommunitySort.LATEST -> LATEST_SORT
                GlowCommunitySort.POPULAR -> POPULAR_SORT
            }
        val page =
            glowCommunityPostJpaRepository.findAll(
                GlowCommunityPostSpecifications.search(query, viewer),
                PageRequest.of(query.page, query.size, sort),
            )
        return page.toSlice()
    }

    override fun findHighlights(limit: Int): List<GlowCommunityPost> =
        glowCommunityPostJpaRepository.findHighlights(PageRequest.of(0, limit))

    override fun findByUserId(
        userId: Long,
        request: OffsetPageRequest,
    ): OffsetSlice<GlowCommunityPost> =
        glowCommunityPostJpaRepository
            .findActiveByUserId(userId, PageRequest.of(request.page, request.size, LATEST_SORT))
            .toSlice()

    override fun findReported(request: OffsetPageRequest): OffsetSlice<GlowCommunityPost> =
        glowCommunityPostJpaRepository
            .findReported(PageRequest.of(request.page, request.size, REPORTED_SORT))
            .toSlice()

    override fun findRecentVisible(limit: Int): List<GlowCommunityPost> =
        glowCommunityPostJpaRepository.findVisible(PageRequest.of(0, limit, LATEST_SORT))

    override fun findPopularVisible(limit: Int): List<GlowCommunityPost> =
        glowCommunityPostJpaRepository.findPopularVisible(PageRequest.of(0, limit))

    private fun Page<GlowCommunityPost>.toSlice(): OffsetSlice<GlowCommunityPost> = OffsetSlice(content, totalElements)

    companion object {
        private val LATEST_SORT: Sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        private val POPULAR_SORT: Sort =
            Sort.by(
                Sort.Order.desc("likeCount"),
                Sort.Order.desc("commentCount"),
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id"),
            )
        private val REPORTED_SORT: Sort = Sort.by(Sort.Order.desc("lastReportedAt"), Sort.Order.desc("id"))
    }
}
