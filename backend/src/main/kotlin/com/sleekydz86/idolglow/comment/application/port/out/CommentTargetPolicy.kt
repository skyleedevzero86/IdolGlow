package com.sleekydz86.idolglow.comment.application.port.out

import com.sleekydz86.idolglow.global.support.ContentTargetType

interface CommentTargetPolicy {
    val targetType: ContentTargetType

    fun checkReadable(
        targetKey: String,
        viewerId: Long?,
        admin: Boolean,
    )

    fun checkWritable(
        targetKey: String,
        userId: Long,
    )

    fun commentAdded(targetKey: String)

    fun commentRemoved(targetKey: String)

    fun ownerOf(targetKey: String): Long? = null

    fun titleOf(targetKey: String): String? = null
}
