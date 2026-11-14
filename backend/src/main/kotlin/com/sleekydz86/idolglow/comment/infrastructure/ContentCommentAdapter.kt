package com.sleekydz86.idolglow.comment.infrastructure

import com.sleekydz86.idolglow.comment.application.dto.ContentCommentTarget
import com.sleekydz86.idolglow.comment.application.port.out.ContentCommentPort
import com.sleekydz86.idolglow.comment.domain.ContentComment
import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetSlice
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component

@Component
class ContentCommentAdapter(
    private val contentCommentJpaRepository: ContentCommentJpaRepository,
) : ContentCommentPort {
    override fun save(comment: ContentComment): ContentComment = contentCommentJpaRepository.save(comment)

    override fun findById(commentId: Long): ContentComment? =
        contentCommentJpaRepository.findById(commentId).orElse(null)

    override fun findAllByIds(commentIds: Collection<Long>): List<ContentComment> =
        if (commentIds.isEmpty()) emptyList() else contentCommentJpaRepository.findAllById(commentIds)

    override fun findThreadRoots(
        target: ContentCommentTarget,
        request: OffsetPageRequest,
        latestFirst: Boolean,
    ): OffsetSlice<ContentComment> {
        val direction = if (latestFirst) Sort.Direction.DESC else Sort.Direction.ASC
        val page =
            contentCommentJpaRepository.findThreadRoots(
                target.type,
                target.key,
                PageRequest.of(request.page, request.size, Sort.by(direction, "createdAt", "id")),
            )
        return OffsetSlice(page.content, page.totalElements)
    }

    override fun findRepliesByRootIds(rootIds: Collection<Long>): List<ContentComment> =
        if (rootIds.isEmpty()) emptyList() else contentCommentJpaRepository.findAllByRootIdIn(rootIds)

    override fun findActiveByUserId(
        userId: Long,
        request: OffsetPageRequest,
    ): OffsetSlice<ContentComment> {
        val page =
            contentCommentJpaRepository.findActiveByUserId(
                userId,
                PageRequest.of(request.page, request.size, Sort.by(Sort.Direction.DESC, "createdAt", "id")),
            )
        return OffsetSlice(page.content, page.totalElements)
    }
}
