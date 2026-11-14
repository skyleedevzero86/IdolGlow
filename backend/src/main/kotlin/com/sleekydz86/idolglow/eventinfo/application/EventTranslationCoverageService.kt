package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventTranslationCoveragePort
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslationLocales
import com.sleekydz86.idolglow.eventinfo.domain.TranslationCoverage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EventTranslationCoverageService(
    private val coveragePort: EventTranslationCoveragePort,
) {
    private val log = LoggerFactory.getLogger(EventTranslationCoverageService::class.java)

    @Transactional(readOnly = true)
    fun coverage(sampleLimit: Int = DEFAULT_SAMPLE_LIMIT): List<TranslationCoverage> {
        val limit = sampleLimit.coerceIn(0, MAX_SAMPLE_LIMIT)
        val totalActive = coveragePort.countActive()
        return EventTranslationLocales.TARGETS.map { locale -> measure(locale, totalActive, limit) }
    }

    private fun measure(
        locale: String,
        totalActive: Long,
        limit: Int,
    ): TranslationCoverage {
        val coverage =
            TranslationCoverage(
                locale = locale,
                totalActive = totalActive,
                visible = coveragePort.countVisible(locale),
                machineOnly = coveragePort.countMachineOnly(locale),
                missing = coveragePort.countMissing(locale),
                missingSamples = coveragePort.findMissing(locale, limit),
            )
        if (!coverage.isConsistent()) {
            log.warn(
                "번역 현황 합계가 맞지 않습니다. locale={} total={} visible={} machineOnly={} missing={}",
                locale,
                totalActive,
                coverage.visible,
                coverage.machineOnly,
                coverage.missing,
            )
        }
        return coverage
    }

    companion object {
        const val DEFAULT_SAMPLE_LIMIT: Int = 20
        const val MAX_SAMPLE_LIMIT: Int = 200
    }
}
