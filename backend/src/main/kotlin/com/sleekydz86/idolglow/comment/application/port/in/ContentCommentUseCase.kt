package com.sleekydz86.idolglow.comment.application.port.`in`

import com.sleekydz86.idolglow.comment.application.dto.ContentCommentResult
import com.sleekydz86.idolglow.comment.application.dto.ContentCommentTarget
import com.sleekydz86.idolglow.comment.application.dto.ContentCommentViewer
import com.sleekydz86.idolglow.comment.application.dto.CreateContentCommentCommand
import com.sleekydz86.idolglow.comment.application.dto.MyReviewResult
import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetPageResult

interface ContentCommentUseCase {
    fun findComments(
        target: ContentCommentTarget,
        viewer: ContentCommentViewer,
        request: OffsetPageRequest,
        latestFirst: Boolean,
    ): OffsetPageResult<ContentCommentResult>

    fun create(command: CreateContentCommentCommand): ContentCommentResult

    fun update(
        commentId: Long,
        userId: Long,
        contentMarkdown: String,
    ): ContentCommentResult

    fun delete(
        commentId: Long,
        viewer: ContentCommentViewer,
    )

    fun findMyReviews(
        userId: Long,
        request: OffsetPageRequest,
    ): OffsetPageResult<MyReviewResult>
}
