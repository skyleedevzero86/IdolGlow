package com.sleekydz86.idolglow.ops.infrastructure

import com.sleekydz86.idolglow.ops.application.port.OpsChangeRequestPort
import com.sleekydz86.idolglow.ops.domain.OpsChangeRequest
import org.springframework.stereotype.Repository

@Repository
class OpsChangeRequestRepositoryImpl(
    private val opsChangeRequestJpaRepository: OpsChangeRequestJpaRepository,
) : OpsChangeRequestPort {
    override fun save(request: OpsChangeRequest): OpsChangeRequest =
        opsChangeRequestJpaRepository.saveAndFlush(request)

    override fun findByIdForUpdate(id: Long): OpsChangeRequest? = opsChangeRequestJpaRepository.findByIdForUpdate(id)

    override fun findRecent(): List<OpsChangeRequest> = opsChangeRequestJpaRepository.findTop50ByOrderByIdDesc()
}
