package com.sleekydz86.idolglow.sdom.application

import com.sleekydz86.idolglow.sdom.application.dto.SdomOptionResponse
import com.sleekydz86.idolglow.sdom.infrastructure.SdomJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SdomQueryService(
    private val sdomJpaRepository: SdomJpaRepository,
) {
    fun findAllActiveOrdered(): List<SdomOptionResponse> =
        sdomJpaRepository.findByUseAtOrderBySortSeqAsc("Y").map { e ->
            SdomOptionResponse(
                domainId = e.domainId,
                domainName = e.domainNm,
                domainPath = e.domainPath,
                description = e.domainDc,
                useYn = e.useAt,
                sortOrder = e.sortSeq ?: 0,
            )
        }
}
