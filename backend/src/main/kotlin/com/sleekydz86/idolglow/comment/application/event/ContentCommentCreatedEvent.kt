package com.sleekydz86.idolglow.comment.application.event

import com.sleekydz86.idolglow.global.support.ContentTargetType

data class ContentCommentCreatedEvent(
    val commentId: Long,
    val actorUserId: Long,
    val targetType: ContentTargetType,
    val targetKey: String,
    val targetTitle: String,
    val targetPath: String?,
    val parentAuthorId: Long?,
)
