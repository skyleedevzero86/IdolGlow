package com.sleekydz86.idolglow.subscription.application.port.out

import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetSlice
import com.sleekydz86.idolglow.subscription.application.dto.MailSendLogSearchCondition
import com.sleekydz86.idolglow.subscription.domain.MailSendLog
import java.time.LocalDateTime

interface MailSendLogPort {
    fun save(log: MailSendLog): MailSendLog

    fun findById(id: Long): MailSendLog?

    fun findLatestByDispatchId(dispatchId: Long): MailSendLog?

    fun search(
        condition: MailSendLogSearchCondition,
        request: OffsetPageRequest,
    ): OffsetSlice<MailSendLog>

    fun deleteCreatedBefore(before: LocalDateTime): Int
}
