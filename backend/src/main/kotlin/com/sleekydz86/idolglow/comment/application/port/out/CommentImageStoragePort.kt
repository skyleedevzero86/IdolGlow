package com.sleekydz86.idolglow.comment.application.port.out

import com.sleekydz86.idolglow.comment.application.dto.CommentImageFile

interface CommentImageStoragePort {
    fun store(image: CommentImageFile): String
}
