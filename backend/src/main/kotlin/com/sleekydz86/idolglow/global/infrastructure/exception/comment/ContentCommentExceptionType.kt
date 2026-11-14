package com.sleekydz86.idolglow.global.infrastructure.exception.comment

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomExceptionType

enum class ContentCommentExceptionType(
    override val errorCode: String,
    override val message: String,
    override val httpStatusCode: Int,
) : CustomExceptionType {
    COMMENT_NOT_FOUND(
        "CONTENT_COMMENT_NOT_FOUND",
        "댓글을 찾을 수 없습니다.",
        NOT_FOUND,
    ),
    NOT_COMMENT_OWNER(
        "CONTENT_COMMENT_NOT_OWNER",
        "본인이 작성한 댓글만 수정하거나 삭제할 수 있습니다.",
        FORBIDDEN,
    ),
    IMAGE_STORAGE_UNAVAILABLE(
        "CONTENT_COMMENT_IMAGE_STORAGE_UNAVAILABLE",
        "이미지 업로드 스토리지를 사용할 수 없습니다.",
        SERVICE_UNAVAILABLE,
    ),
}

private const val FORBIDDEN = 403
private const val NOT_FOUND = 404
private const val SERVICE_UNAVAILABLE = 503
