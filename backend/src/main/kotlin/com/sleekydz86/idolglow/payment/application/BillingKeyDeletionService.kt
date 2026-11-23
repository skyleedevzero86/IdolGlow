package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.notification.application.NotificationCommandService
import com.sleekydz86.idolglow.notification.domain.NotificationType
import com.sleekydz86.idolglow.payment.application.dto.TossBillingMethodResponse
import com.sleekydz86.idolglow.payment.domain.TossBillingKeyDeletionStatus
import com.sleekydz86.idolglow.payment.infrastructure.BillingKeyCrypto
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingMethodJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import tools.jackson.databind.JsonNode
import java.time.LocalDateTime

@Service
class BillingKeyDeletionService(
    private val billingKeyDeletionTxService: BillingKeyDeletionTxService,
    private val methodRepository: TossBillingMethodJpaRepository,
    private val tossPaymentsApiClient: TossPaymentsApiClient,
    private val notificationCommandService: NotificationCommandService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun deactivate(
        userId: Long,
        methodId: Long,
    ): TossBillingMethodResponse {
        val now = LocalDateTime.now()
        val prepared = billingKeyDeletionTxService.prepare(userId, methodId, now)
        if (prepared.notify) {
            notifyReplacementNeeded(prepared.userId)
        }
        val key = prepared.billingKey
        if (prepared.callToss && key != null) {
            billingKeyDeletionTxService.applyRemote(methodId, tossPaymentsApiClient.deleteBillingKey(key), now)
        }
        val saved =
            methodRepository.findById(methodId).orElseThrow {
                IllegalArgumentException("결제 수단을 찾을 수 없습니다. methodId=$methodId")
            }
        return TossBillingMethodResponse.from(saved)
    }

    fun retryDue(now: LocalDateTime) {
        val methodIds =
            methodRepository.findDeletionRetryIds(
                pending = TossBillingKeyDeletionStatus.DELETE_PENDING,
                attemptedBefore = now.minusMinutes(MIN_RETRY_MINUTES),
                pageable = PageRequest.of(0, BATCH_SIZE),
            )
        methodIds.forEach { methodId ->
            runCatching { retryOne(methodId, now) }
                .onFailure { error ->
                    log.warn("빌링키 삭제 재시도 실패 methodId={}", methodId, error)
                }
        }
    }

    fun acceptBillingDeleted(data: JsonNode) {
        val billingKey = data.path("billingKey").asText(null)?.trim()?.takeIf { it.isNotEmpty() }
        if (billingKey == null) {
            log.warn("빌링키 삭제 웹훅에 billingKey가 없습니다.")
            return
        }
        if (methodRepository.findByBillingKeyHash(BillingKeyCrypto.sha256Hex(billingKey)) == null) {
            log.warn("빌링키 삭제 웹훅에 일치하는 카드가 없습니다.")
            return
        }
        val applied = billingKeyDeletionTxService.acceptWebhook(billingKey, LocalDateTime.now())
        if (applied.notify) {
            notifyReplacementNeeded(applied.userId)
        }
    }

    private fun retryOne(
        methodId: Long,
        now: LocalDateTime,
    ) {
        val key = billingKeyDeletionTxService.loadRetryKey(methodId) ?: return
        val remote = tossPaymentsApiClient.deleteBillingKey(key)
        val applied = billingKeyDeletionTxService.applyRemote(methodId, remote, now)
        val requestedAt = methodRepository.findById(methodId).orElse(null)?.deletionRequestedAt
        val stale = requestedAt != null && !requestedAt.isAfter(now.minusHours(STALE_HOURS))
        if (applied.outcome == BillingKeyDeletionOutcome.PENDING && stale) {
            log.error(
                "빌링키 삭제가 24시간 넘게 대기 중, 관리자 확인 필요. methodId={} requestedAt={} httpStatus={}",
                methodId,
                requestedAt,
                remote.httpStatus,
            )
        }
    }

    private fun notifyReplacementNeeded(userId: Long?) {
        val id = userId ?: return
        runCatching {
            notificationCommandService.create(
                userId = id,
                type = NotificationType.PAYMENT_FAILED,
                title = "결제 수단을 다시 등록해 주세요",
                message = "등록된 결제 수단이 삭제되어 다음 자동결제를 진행할 수 없습니다.",
                link = "/billing",
            )
        }.onFailure { error ->
            log.warn("빌링키 삭제 알림 발송 실패 userId={}", id, error)
        }
    }

    private companion object {
        const val BATCH_SIZE = 100
        const val MIN_RETRY_MINUTES = 1L
        const val STALE_HOURS = 24L
    }
}
