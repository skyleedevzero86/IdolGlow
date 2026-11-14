package com.sleekydz86.idolglow.comment.application.dto

import com.sleekydz86.idolglow.global.support.ContentTargetType

data class ContentCommentViewer(
    val userId: Long?,
    val admin: Boolean = false,
) {
    companion object {
        val ANONYMOUS = ContentCommentViewer(userId = null)
    }
}

data class CreateContentCommentCommand(
    val userId: Long,
    val targetType: ContentTargetType,
    val targetKey: String,
    val targetTitle: String?,
    val targetPath: String?,
    val contentMarkdown: String,
    val parentId: Long? = null,
)

data class ContentCommentTarget(
    val type: ContentTargetType,
    val key: String,
)

data class CommentImageFile(
    val bytes: ByteArray,
    val originalFilename: String?,
    val contentType: String,
    val extension: String,
) {
    override fun equals(other: Any?): Boolean =
        other is CommentImageFile &&
            bytes.contentEquals(other.bytes) &&
            originalFilename == other.originalFilename &&
            contentType == other.contentType &&
            extension == other.extension

    override fun hashCode(): Int = bytes.contentHashCode()
}
