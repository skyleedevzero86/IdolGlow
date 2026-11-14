package com.sleekydz86.idolglow.comment.application.port.out

interface ContentCommentAuthorPort {
    fun findNicknames(userIds: Collection<Long>): Map<Long, String>

    fun findAdminIds(userIds: Collection<Long>): Set<Long>
}
