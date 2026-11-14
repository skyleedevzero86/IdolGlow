package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.global.config.AppMailProperties
import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailSettingResult
import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailUnsubscribeNotice
import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailUnsubscribePreviewResult
import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailUnsubscribeResult
import com.sleekydz86.idolglow.subscription.application.dto.SubscribeProductRecommendationMailCommand
import com.sleekydz86.idolglow.subscription.application.port.`in`.ProductRecommendationMailUnsubscribeUseCase
import com.sleekydz86.idolglow.subscription.application.port.`in`.ProductRecommendationMailUseCase
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailMessage
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ProductRecommendationMailService(
    private val productRecommendationMailTxService: ProductRecommendationMailTxService,
    private val productRecommendationMailNoticeComposer: ProductRecommendationMailNoticeComposer,
    private val outboundMailPort: OutboundMailPort,
    private val appMailProperties: AppMailProperties,
) : ProductRecommendationMailUseCase,
    ProductRecommendationMailUnsubscribeUseCase {
    private val log = LoggerFactory.getLogger(ProductRecommendationMailService::class.java)

    override fun findMine(userId: Long): ProductRecommendationMailSettingResult {
        val result = productRecommendationMailTxService.findMine(userId)
        return result
    }

    override fun subscribe(command: SubscribeProductRecommendationMailCommand): ProductRecommendationMailSettingResult {
        val now = LocalDateTime.now()
        return productRecommendationMailTxService.subscribe(command, now)
    }

    override fun unsubscribeMine(userId: Long): ProductRecommendationMailSettingResult {
        val change = productRecommendationMailTxService.unsubscribeByUser(userId, LocalDateTime.now())
        change.notice?.let(::sendUnsubscribedNotice)
        return change.setting
    }

    override fun previewUnsubscribe(token: String?): ProductRecommendationMailUnsubscribePreviewResult {
        val normalized = token?.trim().orEmpty()
        if (normalized.isEmpty()) return ProductRecommendationMailUnsubscribePreviewResult.INVALID
        return productRecommendationMailTxService.preview(normalized)
    }

    override fun unsubscribeByToken(token: String?): ProductRecommendationMailUnsubscribeResult {
        val normalized = token?.trim().orEmpty()
        if (normalized.isNotEmpty()) {
            productRecommendationMailTxService
                .unsubscribeByToken(normalized, LocalDateTime.now())
                ?.let(::sendUnsubscribedNotice)
        }
        return ProductRecommendationMailUnsubscribeResult.ACCEPTED
    }

    private fun sendUnsubscribedNotice(notice: ProductRecommendationMailUnsubscribeNotice) {
        if (!appMailProperties.enabled) return
        runCatching {
            val composed = productRecommendationMailNoticeComposer.composeUnsubscribed(notice)
            outboundMailPort.send(
                OutboundMailMessage(
                    to = notice.email,
                    subject = composed.subject,
                    plainTextBody = composed.plainText,
                    htmlBody = composed.htmlBody,
                ),
            )
        }.onFailure { ex ->
            log.warn(
                "상품 추천 메일 수신 거부 확인 메일 발송 실패: subscriptionId={} cause={}",
                notice.subscriptionId,
                ex.javaClass.simpleName,
            )
        }
    }
}
