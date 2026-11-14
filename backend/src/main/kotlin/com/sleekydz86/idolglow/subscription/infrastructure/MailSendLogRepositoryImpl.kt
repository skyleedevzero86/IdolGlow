package com.sleekydz86.idolglow.subscription.infrastructure

import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetSlice
import com.sleekydz86.idolglow.subscription.application.dto.MailSendLogSearchCondition
import com.sleekydz86.idolglow.subscription.application.port.out.MailSendLogPort
import com.sleekydz86.idolglow.subscription.domain.MailSendLog
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class MailSendLogRepositoryImpl(
    private val mailSendLogJpaRepository: MailSendLogJpaRepository,
) : MailSendLogPort {
    override fun save(log: MailSendLog): MailSendLog = mailSendLogJpaRepository.save(log)

    override fun findById(id: Long): MailSendLog? = mailSendLogJpaRepository.findByIdOrNull(id)

    override fun findLatestByDispatchId(dispatchId: Long): MailSendLog? =
        mailSendLogJpaRepository.findFirstByDispatchIdOrderByIdDesc(dispatchId)

    override fun search(
        condition: MailSendLogSearchCondition,
        request: OffsetPageRequest,
    ): OffsetSlice<MailSendLog> {
        val page =
            mailSendLogJpaRepository.findAll(
                SubscriptionAdminSpecifications.mailLogs(condition),
                PageRequest.of(request.page, request.size, LATEST_SORT),
            )
        return OffsetSlice(page.content, page.totalElements)
    }

    override fun deleteCreatedBefore(before: LocalDateTime): Int = mailSendLogJpaRepository.deleteCreatedBefore(before)

    companion object {
        private val LATEST_SORT: Sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
    }
}
