package com.sleekydz86.idolglow.partner.infrastructure

import com.sleekydz86.idolglow.partner.application.port.out.PartnerCallLogPort
import com.sleekydz86.idolglow.partner.application.port.out.PartnerClientPort
import com.sleekydz86.idolglow.partner.domain.PartnerCallLog
import com.sleekydz86.idolglow.partner.domain.PartnerClient
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface PartnerClientJpaRepository : JpaRepository<PartnerClient, Long> {
    fun findByKeyHash(keyHash: String): PartnerClient?
}

interface PartnerCallLogJpaRepository : JpaRepository<PartnerCallLog, Long>

@Repository
class PartnerClientAdapter(
    private val repository: PartnerClientJpaRepository,
) : PartnerClientPort {
    override fun save(client: PartnerClient): PartnerClient = repository.saveAndFlush(client)

    override fun findByKeyHash(keyHash: String): PartnerClient? = repository.findByKeyHash(keyHash)

    override fun findById(id: Long): PartnerClient? = repository.findById(id).orElse(null)

    override fun findAll(): List<PartnerClient> = repository.findAll()
}

@Repository
class PartnerCallLogAdapter(
    private val repository: PartnerCallLogJpaRepository,
) : PartnerCallLogPort {
    override fun save(log: PartnerCallLog): PartnerCallLog = repository.save(log)
}
