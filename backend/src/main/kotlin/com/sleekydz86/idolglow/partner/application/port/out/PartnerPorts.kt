package com.sleekydz86.idolglow.partner.application.port.out

import com.sleekydz86.idolglow.partner.domain.PartnerCallLog
import com.sleekydz86.idolglow.partner.domain.PartnerClient

interface PartnerClientPort {
    fun save(client: PartnerClient): PartnerClient

    fun findByKeyHash(keyHash: String): PartnerClient?

    fun findById(id: Long): PartnerClient?

    fun findAll(): List<PartnerClient>
}

interface PartnerCallLogPort {
    fun save(log: PartnerCallLog): PartnerCallLog
}
