package com.sleekydz86.idolglow.translation.application

import com.sleekydz86.idolglow.global.adapter.resolver.AuthenticatedUserIdResolver
import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import com.sleekydz86.idolglow.translation.application.dto.TranslationRowResult
import com.sleekydz86.idolglow.translation.application.dto.TranslationSummaryResult
import com.sleekydz86.idolglow.translation.application.port.out.ContentTranslationPort
import com.sleekydz86.idolglow.translation.application.port.out.TranslationSourcePort
import com.sleekydz86.idolglow.translation.domain.ContentTranslation
import com.sleekydz86.idolglow.translation.domain.TranslationContentType
import com.sleekydz86.idolglow.translation.domain.TranslationStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime

@Service
class TranslationAdminService(
    private val contentTranslationPort: ContentTranslationPort,
    private val sourcePort: TranslationSourcePort,
    private val userIdResolver: AuthenticatedUserIdResolver,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(TranslationAdminService::class.java)

    @Transactional(readOnly = true)
    fun list(
        status: TranslationStatus,
        type: TranslationContentType?,
        page: Int,
        size: Int,
    ): List<TranslationRowResult> {
        val safeSize = size.coerceIn(1, MAX_PAGE_SIZE)
        return contentTranslationPort
            .findByStatus(status, type, page.coerceAtLeast(0), safeSize)
            .map { toResult(it, withSource = false) }
    }

    @Transactional(readOnly = true)
    fun detail(id: Long): TranslationRowResult = toResult(load(id), withSource = true)

    @Transactional(readOnly = true)
    fun summary(): List<TranslationSummaryResult> =
        contentTranslationPort.summary().map { TranslationSummaryResult(it.type, it.locale, it.status.name, it.count) }

    @Transactional
    fun approve(id: Long): TranslationRowResult {
        val adminId = userIdResolver.resolveRequired()
        val row = load(id)
        row.approve(adminId, now())
        contentTranslationPort.save(row)
        log.info("번역을 승인했습니다. id={} adminId={}", id, adminId)
        return toResult(row, withSource = false)
    }

    @Transactional
    fun edit(
        id: Long,
        fields: Map<String, String>,
    ): TranslationRowResult {
        val adminId = userIdResolver.resolveRequired()
        val row = load(id)
        val source =
            sourcePort.load(row.contentType, row.contentKey)
                ?: throw IllegalArgumentException("원문을 찾을 수 없어 번역을 고칠 수 없습니다. id=$id")
        val cleaned = fields.mapValues { it.value.trim() }
        require(cleaned.keys == source.fields.keys) {
            "원문과 같은 항목을 모두 입력해야 합니다. 항목=${source.fields.keys.joinToString(",")}"
        }
        require(cleaned.values.all { it.isNotEmpty() }) { "비어 있는 번역 항목이 있습니다." }
        row.rehash(source.hash())
        row.editManually(objectMapper.writeValueAsString(cleaned), adminId, now())
        contentTranslationPort.save(row)
        log.info("번역을 직접 수정했습니다. id={} adminId={}", id, adminId)
        return toResult(row, withSource = false)
    }

    @Transactional
    fun requeue(id: Long): TranslationRowResult {
        val row = load(id)
        row.requeue()
        contentTranslationPort.save(row)
        log.info("번역을 다시 대기열에 넣었습니다. id={}", id)
        return toResult(row, withSource = false)
    }

    private fun load(id: Long): ContentTranslation =
        contentTranslationPort.findById(id) ?: throw IllegalArgumentException("번역을 찾을 수 없습니다. id=$id")

    private fun toResult(
        row: ContentTranslation,
        withSource: Boolean,
    ): TranslationRowResult =
        TranslationRowResult(
            id = row.id,
            type = row.contentType,
            key = row.contentKey,
            locale = row.locale,
            status = row.status.name,
            origin = row.origin?.name,
            model = row.model,
            promptVersion = row.promptVersion,
            checkNotes = row.checkNotes,
            attempts = row.attempts,
            reportCount = row.reportCount,
            fields = readFields(row.fieldsJson),
            source = if (withSource) sourcePort.load(row.contentType, row.contentKey)?.fields.orEmpty() else emptyMap(),
        )

    private fun readFields(json: String?): Map<String, String> =
        json?.let { runCatching { objectMapper.readValue(it, FIELDS_TYPE) }.getOrNull() }.orEmpty()

    private fun now(): LocalDateTime = LocalDateTime.now(IdolGlowDateTimes.ZONE)

    companion object {
        private const val MAX_PAGE_SIZE = 100
        private val FIELDS_TYPE = object : TypeReference<Map<String, String>>() {}
    }
}
