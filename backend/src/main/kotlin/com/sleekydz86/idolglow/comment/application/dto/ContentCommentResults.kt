package com.sleekydz86.idolglow.comment.application.dto

import java.time.LocalDateTime

data class ContentCommentResult(
    val id: Long,
    val targetType: String,
    val targetKey: String,
    val parentId: Long?,
    val depth: Int,
    val contentMarkdown: String,
    val authorName: String?,
    val authorId: Long?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val mine: Boolean,
    val deleted: Boolean,
    val replies: List<ContentCommentResult>,
)

data class MyReviewResult(
    val commentId: Long,
    val targetType: String,
    val targetKey: String,
    val targetTitle: String,
    val targetPath: String?,
    val contentMarkdown: String,
    val excerpt: String,
    val parentId: Long?,
    val parentExcerpt: String?,
    val depth: Int,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
)

data class CommentImageUploadResult(
    val url: String,
)
