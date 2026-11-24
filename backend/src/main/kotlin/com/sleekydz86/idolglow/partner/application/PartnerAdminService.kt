package com.sleekydz86.idolglow.partner.application

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.partner.PartnerExceptionType
import com.sleekydz86.idolglow.partner.application.dto.PartnerIssued
import com.sleekydz86.idolglow.partner.application.dto.PartnerSummary
import com.sleekydz86.idolglow.partner.application.port.out.PartnerClientPort
import com.sleekydz86.idolglow.partner.domain.PartnerClient
import com.sleekydz86.idolglow.partner.domain.PartnerKey
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PartnerAdminService(
    private val partnerClientPort: PartnerClientPort,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun issue(
        name: String,
        locales: List<String>,
        rateLimitPerMinute: Int,
    ): PartnerIssued {
        val rawKey = PartnerKey.generate()
        val client = PartnerClient.issue(name, PartnerKey.hash(rawKey), locales, rateLimitPerMinute)
        val saved = partnerClientPort.save(client)
        log.info("파트너 키 발급. partnerId={} locales={}", saved.id, saved.locales)
        return PartnerIssued(saved.id, saved.name, rawKey, saved.contractedLocales(), saved.rateLimitPerMinute)
    }

    @Transactional(readOnly = true)
    fun list(): List<PartnerSummary> = partnerClientPort.findAll().map { it.toSummary() }

    @Transactional
    fun changeEnabled(
        id: Long,
        enabled: Boolean,
    ): PartnerSummary {
        val client = partnerClientPort.findById(id) ?: throw CustomException(PartnerExceptionType.PARTNER_NOT_FOUND)
        client.changeEnabled(enabled)
        log.info("파트너 사용 여부 변경. partnerId={} enabled={}", id, enabled)
        return client.toSummary()
    }

    private fun PartnerClient.toSummary(): PartnerSummary =
        PartnerSummary(id, name, contractedLocales(), rateLimitPerMinute, enabled)
}
