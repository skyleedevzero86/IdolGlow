package com.sleekydz86.idolglow.partner.application

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.partner.PartnerExceptionType
import com.sleekydz86.idolglow.global.infrastructure.exception.partner.PartnerRateLimitedException
import com.sleekydz86.idolglow.partner.application.port.out.PartnerCallLogPort
import com.sleekydz86.idolglow.partner.application.port.out.PartnerClientPort
import com.sleekydz86.idolglow.partner.domain.PartnerCallLog
import com.sleekydz86.idolglow.partner.domain.PartnerClient
import com.sleekydz86.idolglow.partner.domain.PartnerKey
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDateTime

@Service
class PartnerAccessService(
    private val partnerClientPort: PartnerClientPort,
    private val partnerCallLogPort: PartnerCallLogPort,
    private val partnerRateLimiter: PartnerRateLimiter,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    internal var clock: Clock = Clock.systemDefaultZone()

    fun authorize(
        rawKey: String?,
        path: String,
    ): PartnerClient {
        val client = findClient(rawKey)
        requireEnabled(client, path)
        requireWithinLimit(client, path)
        return client
    }

    private fun findClient(rawKey: String?): PartnerClient {
        val key = rawKey?.trim()?.takeIf { it.isNotEmpty() } ?: throw CustomException(PartnerExceptionType.KEY_MISSING)
        return partnerClientPort.findByKeyHash(PartnerKey.hash(key))
            ?: throw CustomException(PartnerExceptionType.KEY_INVALID)
    }

    private fun requireEnabled(
        client: PartnerClient,
        path: String,
    ) {
        if (client.enabled) return
        record(client, path, PartnerExceptionType.DISABLED.httpStatusCode, 0)
        throw CustomException(PartnerExceptionType.DISABLED)
    }

    private fun requireWithinLimit(
        client: PartnerClient,
        path: String,
    ) {
        val decision = partnerRateLimiter.acquire(client.id, client.rateLimitPerMinute)
        if (!decision.allowed) {
            record(client, path, PartnerExceptionType.RATE_LIMITED.httpStatusCode, 0)
            log.warn("파트너 호출 한도 초과. partnerId={} path={} limit={}", client.id, path, client.rateLimitPerMinute)
            throw PartnerRateLimitedException(decision.retryAfterSeconds)
        }
    }

    fun record(
        client: PartnerClient,
        path: String,
        status: Int,
        itemCount: Int,
    ) {
        partnerCallLogPort.save(
            PartnerCallLog(
                partnerId = client.id,
                path = path,
                status = status,
                itemCount = itemCount,
                calledAt = LocalDateTime.now(clock),
            ),
        )
    }
}
