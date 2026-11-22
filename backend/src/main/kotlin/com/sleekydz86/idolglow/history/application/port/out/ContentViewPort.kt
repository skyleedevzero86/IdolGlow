package com.sleekydz86.idolglow.history.application.port.out

import com.sleekydz86.idolglow.global.support.ContentTargetType
import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetSlice
import com.sleekydz86.idolglow.history.domain.UserContentView

interface ContentViewPort {
    fun findByUserAndContent(
        userId: Long,
        contentType: ContentTargetType,
        contentKey: String,
    ): UserContentView?

    fun save(view: UserContentView): UserContentView

    fun findById(viewId: Long): UserContentView?

    fun findByUserId(
        userId: Long,
        request: OffsetPageRequest,
    ): OffsetSlice<UserContentView>

    fun delete(view: UserContentView)

    fun deleteAllByUserId(userId: Long)

    fun trimToLatest(
        userId: Long,
        keep: Int,
    )
}
