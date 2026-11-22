package com.sleekydz86.idolglow.translation.application

import com.sleekydz86.idolglow.translation.application.dto.LookupStatus
import com.sleekydz86.idolglow.translation.application.dto.TranslationLookupResult
import com.sleekydz86.idolglow.translation.application.dto.TranslationTarget
import com.sleekydz86.idolglow.translation.application.port.out.ContentTranslationPort
import com.sleekydz86.idolglow.translation.application.port.out.TranslationSourcePort
import com.sleekydz86.idolglow.translation.domain.ContentTranslation
import com.sleekydz86.idolglow.translation.domain.TranslationLocales
import com.sleekydz86.idolglow.translation.domain.TranslationOrigin
import com.sleekydz86.idolglow.translation.domain.TranslationStatus
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper

@Service
class TranslationQueryService(
    private val contentTranslationPort: ContentTranslationPort,
    private val sourcePort: TranslationSourcePort,
    private val enqueueService: TranslationEnqueueService,
    private val props: TranslationProperties,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(TranslationQueryService::class.java)

    fun lookup(
        requestedLocale: String,
        targets: List<TranslationTarget>,
    ): List<TranslationLookupResult> {
        require(targets.size <= props.lookupLimit) { "번역 조회는 한 번에 ${props.lookupLimit}건까지 가능합니다." }
        val distinct = targets.distinct()
        val order = TranslationLocales.servingOrder(requestedLocale)
        if (order.isEmpty()) return distinct.map { original(it, requestedLocale) }
        val rows =
            distinct
                .groupBy { it.type }
                .flatMap { (type, items) -> contentTranslationPort.findByTargets(type, items.map { it.key }) }
                .groupBy { TranslationTarget(it.contentType, it.contentKey) }
        var enqueueBudget = props.lazyEnqueueLimit
        return distinct.map { target ->
            val targetRows = rows[target].orEmpty()
            val served =
                order.firstNotNullOfOrNull { locale -> targetRows.firstOrNull { it.locale == locale && it.servable } }
            when {
                served != null -> translated(target, requestedLocale, served)
                targetRows.any { it.status == TranslationStatus.PENDING } -> pending(target, requestedLocale)
                targetRows.isEmpty() && enqueueBudget > 0 -> {
                    enqueueBudget -= 1
                    if (enqueueFromView(target)) pending(target, requestedLocale) else original(target, requestedLocale)
                }
                else -> original(target, requestedLocale)
            }
        }
    }

    private fun enqueueFromView(target: TranslationTarget): Boolean {
        val source =
            sourcePort
                .load(target.type, target.key)
                ?.takeIf { it.publicVisible && it.fields.isNotEmpty() }
                ?: return false
        return runCatching { enqueueService.ensure(source, TranslationProperties.VIEW_PRIORITY) > 0 }
            .recover { error ->
                if (error is DataIntegrityViolationException) {
                    true
                } else {
                    log.warn("번역 대기 등록에 실패했습니다. type={} key={}", target.type, target.key, error)
                    false
                }
            }.getOrDefault(false)
    }

    private fun translated(
        target: TranslationTarget,
        requestedLocale: String,
        row: ContentTranslation,
    ): TranslationLookupResult =
        TranslationLookupResult(
            type = target.type,
            key = target.key,
            status = LookupStatus.TRANSLATED,
            requestedLocale = requestedLocale,
            resolvedLocale = row.locale,
            machine = row.origin != TranslationOrigin.MANUAL,
            translationId = row.id,
            fields = readFields(row.fieldsJson),
        )

    private fun pending(
        target: TranslationTarget,
        requestedLocale: String,
    ): TranslationLookupResult = empty(target, requestedLocale, LookupStatus.PENDING)

    private fun original(
        target: TranslationTarget,
        requestedLocale: String,
    ): TranslationLookupResult = empty(target, requestedLocale, LookupStatus.ORIGINAL)

    private fun empty(
        target: TranslationTarget,
        requestedLocale: String,
        status: LookupStatus,
    ): TranslationLookupResult =
        TranslationLookupResult(
            type = target.type,
            key = target.key,
            status = status,
            requestedLocale = requestedLocale,
            resolvedLocale = TranslationLocales.KOREAN,
            machine = false,
            translationId = null,
            fields = emptyMap(),
        )

    private fun readFields(json: String?): Map<String, String> =
        json?.let { runCatching { objectMapper.readValue(it, FIELDS_TYPE) }.getOrNull() }.orEmpty()

    companion object {
        private val FIELDS_TYPE = object : TypeReference<Map<String, String>>() {}
    }
}
