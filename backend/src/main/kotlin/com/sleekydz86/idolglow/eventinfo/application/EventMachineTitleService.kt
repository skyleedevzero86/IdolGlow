package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventTitleTranslatorPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventTranslationPort
import com.sleekydz86.idolglow.eventinfo.domain.EventMachineTitle
import com.sleekydz86.idolglow.eventinfo.domain.EventTitleTarget
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslationLocales
import com.sleekydz86.idolglow.translation.domain.TranslationGlossaryTerm
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class EventMachineTitleService(
    private val eventTranslationPort: EventTranslationPort,
    private val eventTitleTranslatorPort: EventTitleTranslatorPort,
    private val eventTranslationTxService: EventTranslationTxService,
    private val properties: EventMachineTitleProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun fill(
        from: LocalDate,
        to: LocalDate,
    ): Map<String, Int> {
        val ready = properties.enabled && eventTitleTranslatorPort.available()
        if (!ready) {
            log.warn("AI 공연 제목 번역을 건너뜁니다. enabled={} from={} to={}", properties.enabled, from, to)
            return emptyMap()
        }
        return EventTranslationLocales.TARGETS.associateWith { fillLocale(it, from, to) }
    }

    private fun fillLocale(
        locale: String,
        from: LocalDate,
        to: LocalDate,
    ): Int {
        val targets =
            eventTranslationPort.findUntranslatedFestivals(
                TourEventTranslationCollector.TOUR_SOURCE,
                locale,
                from,
                to,
                properties.maxPerLocale,
            )
        if (targets.size >= properties.maxPerLocale) {
            log.warn(
                "AI 공연 제목 번역 대상이 언어당 상한에 닿았습니다. 남은 공연은 다음 실행으로 넘어갑니다 locale={} limit={}",
                locale,
                properties.maxPerLocale,
            )
        }
        val glossary = eventTitleTranslatorPort.glossary(locale)
        val created = targets.count { translateOne(locale, it, glossary) }
        log.info(
            "AI 공연 제목 번역 결과 locale={} targets={} created={} failed={}",
            locale,
            targets.size,
            created,
            targets.size - created,
        )
        return created
    }

    private fun translateOne(
        locale: String,
        target: EventTitleTarget,
        glossary: List<TranslationGlossaryTerm>,
    ): Boolean {
        val response = eventTitleTranslatorPort.translate(locale, target.koreanTitle, glossary)?.trim().orEmpty()
        val problems =
            if (response.isEmpty()) {
                listOf("NO_RESPONSE")
            } else {
                EventMachineTitle.check(locale, target.koreanTitle, response, glossary)
            }
        if (problems.isNotEmpty()) {
            log.info("AI 공연 제목을 저장하지 않습니다 globalId={} locale={} reasons={}", target.globalId, locale, problems)
            eventTranslationTxService.recordMachineFailure(target.globalId, locale, problems.joinToString(","))
            return false
        }
        return save(target, locale, EventMachineTitle.compose(locale, response, target.koreanTitle))
    }

    private fun save(
        target: EventTitleTarget,
        locale: String,
        title: String,
    ): Boolean =
        try {
            eventTranslationTxService.upsertMachine(target.globalId, locale, title)
        } catch (e: DataIntegrityViolationException) {
            log.warn("AI 공연 제목 저장이 다른 저장과 겹쳐 건너뜁니다 globalId={} locale={}", target.globalId, locale, e)
            false
        }
}
