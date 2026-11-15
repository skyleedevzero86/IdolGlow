package com.sleekydz86.idolglow.translation.application

import com.sleekydz86.idolglow.translation.application.port.out.ContentTranslationPort
import com.sleekydz86.idolglow.translation.application.port.out.TranslationSourcePort
import com.sleekydz86.idolglow.translation.domain.SourceContent
import com.sleekydz86.idolglow.translation.domain.TranslationContentType
import com.sleekydz86.idolglow.translation.domain.TranslationStatus
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service

@Service
class TranslationScanService(
    private val sourcePort: TranslationSourcePort,
    private val contentTranslationPort: ContentTranslationPort,
    private val enqueueService: TranslationEnqueueService,
    private val props: TranslationProperties,
) {
    private val log = LoggerFactory.getLogger(TranslationScanService::class.java)

    fun scan(): Map<TranslationContentType, Int> =
        TranslationContentType.entries.associateWith { type ->
            runCatching { if (type.official) scanOfficial(type) else refreshViewed(type) }
                .onFailure { log.warn("번역 대상 스캔에 실패했습니다. type={}", type, it) }
                .getOrDefault(0)
        }

    private fun scanOfficial(type: TranslationContentType): Int {
        var offset = 0
        var queued = 0
        while (true) {
            val page = sourcePort.page(type, offset, props.scanPageSize)
            page.items
                .filter { it.publicVisible }
                .forEach { source -> queued += ensureQuietly(source) }
            if (page.scanned < props.scanPageSize) return queued
            offset += page.scanned
        }
    }

    private fun refreshViewed(type: TranslationContentType): Int {
        var afterId = 0L
        var changed = 0
        while (true) {
            val rows = contentTranslationPort.findPageByType(type, afterId, props.scanPageSize)
            rows
                .filter { it.status != TranslationStatus.SKIPPED }
                .groupBy { it.contentKey }
                .forEach { (key, targetRows) ->
                    val source = sourcePort.load(type, key)
                    changed +=
                        when {
                            source == null || !source.publicVisible -> enqueueService.retire(type, key)
                            targetRows.any { it.sourceHash != source.hash() } -> ensureQuietly(source)
                            else -> 0
                        }
                }
            if (rows.size < props.scanPageSize) return changed
            afterId = rows.maxOf { it.id }
        }
    }

    private fun ensureQuietly(source: SourceContent): Int =
        runCatching { enqueueService.ensure(source, TranslationProperties.SCAN_PRIORITY) }
            .recover { if (it is DataIntegrityViolationException) 0 else throw it }
            .getOrThrow()
}
