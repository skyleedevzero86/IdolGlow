package com.sleekydz86.idolglow.translation.application

import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import com.sleekydz86.idolglow.translation.application.port.out.ContentTranslationPort
import com.sleekydz86.idolglow.translation.application.port.out.EngineRequest
import com.sleekydz86.idolglow.translation.application.port.out.JudgeRequest
import com.sleekydz86.idolglow.translation.application.port.out.TranslationEnginePort
import com.sleekydz86.idolglow.translation.domain.GuardInput
import com.sleekydz86.idolglow.translation.domain.TranslationGuard
import com.sleekydz86.idolglow.translation.domain.TranslationProtector
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDateTime

data class TranslationWorkSummary(
    val engineAvailable: Boolean,
    val outcomes: Map<String, Int>,
) {
    val processed: Int = outcomes.values.sum()
}

@Service
class TranslationWorkerService(
    private val contentTranslationPort: ContentTranslationPort,
    private val txService: TranslationTxService,
    private val engine: TranslationEnginePort,
    private val props: TranslationProperties,
) {
    internal var clock: Clock = Clock.system(IdolGlowDateTimes.ZONE)

    fun runOnce(): TranslationWorkSummary {
        if (!props.enabled || !engine.available()) return TranslationWorkSummary(false, emptyMap())
        val due = contentTranslationPort.findDue(now(), props.batchSize)
        val outcomes = due.map { process(it.id) }.groupingBy { it }.eachCount()
        return TranslationWorkSummary(true, outcomes)
    }

    private fun process(id: Long): String {
        val prepared = txService.prepare(id) ?: return TranslationTxService.OUTCOME_SKIPPED
        return txService.complete(prepared, translate(prepared), now())
    }

    private fun translate(prepared: PreparedTranslation): TranslationOutcome {
        val protectedFields = prepared.fields.mapValues { (_, value) -> TranslationProtector.protect(value) }
        val response =
            engine.translate(
                EngineRequest(prepared.locale, protectedFields.mapValues { it.value.text }, prepared.glossary),
            )
        val raw =
            response?.fields
                ?: return TranslationOutcome.EngineFailed(if (response == null) ENGINE_FAILED else PARSE_FAILED)
        val guard =
            TranslationGuard.check(
                GuardInput(
                    locale = prepared.locale,
                    source = protectedFields.mapValues { it.value.text },
                    translated = raw,
                    tokenCounts = protectedFields.mapValues { it.value.tokens.size },
                    glossary = prepared.glossary,
                ),
            )
        val restored =
            raw
                .filterKeys { it in protectedFields }
                .mapValues { (field, value) ->
                    TranslationProtector.restore(value, protectedFields.getValue(field).tokens)
                }
        val judgeScore =
            if (guard.hard.isEmpty() && props.judgeEnabled) {
                engine.judge(JudgeRequest(prepared.locale, prepared.fields, restored))
            } else {
                null
            }
        return TranslationOutcome.Translated(restored, guard, response.model, judgeScore)
    }

    private fun now(): LocalDateTime = LocalDateTime.now(clock)

    companion object {
        private const val ENGINE_FAILED = "engine_failed"
        private const val PARSE_FAILED = "parse_failed"
    }
}
