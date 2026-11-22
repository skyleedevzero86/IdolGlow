package com.sleekydz86.idolglow.comment.application.port.`in`

import com.sleekydz86.idolglow.comment.application.dto.CommentImageUploadResult

interface CommentImageUploadUseCase {
    fun upload(
        bytes: ByteArray,
        originalFilename: String?,
        contentType: String?,
    ): CommentImageUploadResult
}
