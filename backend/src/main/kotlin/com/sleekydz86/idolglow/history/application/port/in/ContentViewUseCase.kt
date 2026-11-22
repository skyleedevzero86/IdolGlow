package com.sleekydz86.idolglow.history.application.port.`in`

import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetPageResult
import com.sleekydz86.idolglow.history.application.dto.ContentViewResult
import com.sleekydz86.idolglow.history.application.dto.RecordContentViewCommand

interface ContentViewUseCase {
    fun record(command: RecordContentViewCommand): ContentViewResult

    fun findMine(
        userId: Long,
        request: OffsetPageRequest,
    ): OffsetPageResult<ContentViewResult>

    fun clear(userId: Long)

    fun delete(
        userId: Long,
        viewId: Long,
    )
}
