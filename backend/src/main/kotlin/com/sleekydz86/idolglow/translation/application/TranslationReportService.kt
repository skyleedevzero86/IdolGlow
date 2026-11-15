package com.sleekydz86.idolglow.translation.application

import com.sleekydz86.idolglow.global.adapter.resolver.AuthenticatedUserIdResolver
import com.sleekydz86.idolglow.translation.application.port.out.ContentTranslationPort
import com.sleekydz86.idolglow.translation.application.port.out.TranslationReportPort
import com.sleekydz86.idolglow.translation.domain.TranslationReport
import com.sleekydz86.idolglow.translation.domain.TranslationStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class TranslationReportResult(
    val translationId: Long,
    val reportCount: Int,
    val hidden: Boolean,
)

@Service
class TranslationReportService(
    private val contentTranslationPort: ContentTranslationPort,
    private val reportPort: TranslationReportPort,
    private val userIdResolver: AuthenticatedUserIdResolver,
    private val props: TranslationProperties,
) {
    private val log = LoggerFactory.getLogger(TranslationReportService::class.java)

    @Transactional
    fun report(translationId: Long): TranslationReportResult {
        val userId = userIdResolver.resolveRequired()
        val row =
            contentTranslationPort.findByIdForUpdate(translationId)
                ?: throw IllegalArgumentException("번역을 찾을 수 없습니다. id=$translationId")
        if (reportPort.exists(translationId, userId)) {
            return TranslationReportResult(translationId, row.reportCount, row.status != TranslationStatus.VISIBLE)
        }
        require(row.status == TranslationStatus.VISIBLE) { "화면에 보이는 번역만 신고할 수 있습니다." }
        reportPort.save(TranslationReport(translationId = translationId, userId = userId))
        val hidden = row.report(props.reportThreshold)
        contentTranslationPort.save(row)
        if (hidden) log.info("신고가 쌓여 번역을 숨기고 검수 대기로 돌렸습니다. id={} count={}", translationId, row.reportCount)
        return TranslationReportResult(translationId, row.reportCount, hidden)
    }
}
