package com.sleekydz86.idolglow.comment.application.port.out

import com.sleekydz86.idolglow.comment.application.dto.ContentCommentTarget
import com.sleekydz86.idolglow.comment.domain.ContentComment
import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetSlice

interface ContentCommentPort {
    fun save(comment: ContentComment): ContentComment

    fun findById(commentId: Long): ContentComment?

    fun findAllByIds(commentIds: Collection<Long>): List<ContentComment>

    fun findThreadRoots(
        target: ContentCommentTarget,
        request: OffsetPageRequest,
        latestFirst: Boolean,
    ): OffsetSlice<ContentComment>

    fun findRepliesByRootIds(rootIds: Collection<Long>): List<ContentComment>

    fun findActiveByUserId(
        userId: Long,
        request: OffsetPageRequest,
    ): OffsetSlice<ContentComment>
}
