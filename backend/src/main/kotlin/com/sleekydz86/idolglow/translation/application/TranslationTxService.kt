package com.sleekydz86.idolglow.translation.application

import com.sleekydz86.idolglow.translation.application.port.out.ContentTranslationPort
import com.sleekydz86.idolglow.translation.application.port.out.TranslationGlossaryPort
import com.sleekydz86.idolglow.translation.application.port.out.TranslationSourcePort
import com.sleekydz86.idolglow.translation.domain.ContentTranslation
import com.sleekydz86.idolglow.translation.domain.GuardResult
import com.sleekydz86.idolglow.translation.domain.TranslationContentType
import com.sleekydz86.idolglow.translation.domain.TranslationGlossaryTerm
import com.sleekydz86.idolglow.translation.domain.TranslationStatus
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime

data class PreparedTranslation(
    val id: Long,
    val hash: String,
    val type: TranslationContentType,
    val locale: String,
    val fields: Map<String, String>,
    val glossary: List<TranslationGlossaryTerm>,
)

sealed class TranslationOutcome {
    data class EngineFailed(
        val reason: String,
    ) : TranslationOutcome()

    data class Translated(
        val fields: Map<String, String>,
        val guard: GuardResult,
        val model: String,
        val judgeScore: Int?,
    ) : TranslationOutcome()
}

@Service
class TranslationTxService(
    private val contentTranslationPort: ContentTranslationPort,
    private val sourcePort: TranslationSourcePort,
    private val glossaryPort: TranslationGlossaryPort,
    private val props: TranslationProperties,
    private val objectMapper: ObjectMapper,
    private val meterRegistry: MeterRegistry,
) {
    @Transactional
    fun prepare(id: Long): PreparedTranslation? {
        val row = contentTranslationPort.findById(id)?.takeIf { it.status == TranslationStatus.PENDING } ?: return null
        val source =
            sourcePort
                .load(row.contentType, row.contentKey)
                ?.takeIf { it.publicVisible && it.fields.isNotEmpty() }
        val prepared =
            when {
                source == null -> {
                    row.skip("source_unavailable")
                    count(OUTCOME_SKIPPED)
                    null
                }
                source.length > props.maxSourceChars -> {
                    row.rehash(source.hash())
                    row.holdForReview("too_long:${source.length}")
                    count(OUTCOME_REVIEW)
                    null
                }
                else -> {
                    row.rehash(source.hash())
                    PreparedTranslation(
                        id = row.id,
                        hash = row.sourceHash,
                        type = row.contentType,
                        locale = row.locale,
                        fields = source.fields,
                        glossary = glossaryPort.findAll().filter { it.appliesTo(row.locale) },
                    )
                }
            }
        contentTranslationPort.save(row)
        return prepared
    }

    @Transactional
    fun complete(
        prepared: PreparedTranslation,
        outcome: TranslationOutcome,
        now: LocalDateTime,
    ): String {
        val row =
            contentTranslationPort
                .findById(prepared.id)
                ?.takeIf { it.status == TranslationStatus.PENDING && it.sourceHash == prepared.hash }
                ?: return OUTCOME_STALE
        val label =
            when (outcome) {
                is TranslationOutcome.EngineFailed -> {
                    row.markRetry(outcome.reason, now, props.maxAttempts)
                    retryLabel(row.status)
                }
                is TranslationOutcome.Translated -> applyTranslated(row, prepared, outcome, now)
            }
        contentTranslationPort.save(row)
        count(label)
        return label
    }

    private fun applyTranslated(
        row: ContentTranslation,
        prepared: PreparedTranslation,
        outcome: TranslationOutcome.Translated,
        now: LocalDateTime,
    ): String {
        (outcome.guard.hard + outcome.guard.soft).forEach { rule ->
            meterRegistry.counter(CHECK_METRIC, "rule", rule.substringBefore(':')).increment()
        }
        if (outcome.guard.hard.isNotEmpty()) {
            row.markRetry("check:" + outcome.guard.hard.joinToString(","), now, props.maxAttempts)
            return retryLabel(row.status)
        }
        val notes = outcome.guard.soft.toMutableList()
        outcome.judgeScore?.takeIf { it < props.judgeMinScore }?.let { notes += "judge:$it" }
        val reviewRequired = prepared.type in props.reviewRequiredTypes
        if (reviewRequired && notes.isEmpty()) notes += "review_required"
        val next = if (notes.isEmpty()) TranslationStatus.VISIBLE else TranslationStatus.REVIEW
        row.markTranslated(
            fields = objectMapper.writeValueAsString(outcome.fields),
            next = next,
            modelName = outcome.model,
            version = TranslationPrompt.VERSION,
            notes = notes.takeIf { it.isNotEmpty() }?.joinToString(","),
            now = now,
        )
        return if (next == TranslationStatus.VISIBLE) OUTCOME_VISIBLE else OUTCOME_REVIEW
    }

    private fun retryLabel(status: TranslationStatus): String = if (status == TranslationStatus.FAILED) OUTCOME_FAILED else OUTCOME_RETRY

    private fun count(label: String) {
        meterRegistry.counter(PROCESSED_METRIC, "outcome", label).increment()
    }

    companion object {
        const val PROCESSED_METRIC: String = "idolglow.translation.processed"
        const val CHECK_METRIC: String = "idolglow.translation.check"
        const val OUTCOME_VISIBLE: String = "visible"
        const val OUTCOME_REVIEW: String = "review"
        const val OUTCOME_RETRY: String = "retry"
        const val OUTCOME_FAILED: String = "failed"
        const val OUTCOME_SKIPPED: String = "skipped"
        const val OUTCOME_STALE: String = "stale"
    }
}
