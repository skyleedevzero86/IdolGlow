package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventTitleTranslatorPort
import com.sleekydz86.idolglow.translation.application.port.out.EngineRequest
import com.sleekydz86.idolglow.translation.application.port.out.TranslationEnginePort
import com.sleekydz86.idolglow.translation.application.port.out.TranslationGlossaryPort
import com.sleekydz86.idolglow.translation.domain.TranslationGlossaryTerm
import org.springframework.stereotype.Component

@Component
class EventTitleTranslatorAdapter(
    private val engine: TranslationEnginePort,
    private val glossaryPort: TranslationGlossaryPort,
) : EventTitleTranslatorPort {
    override fun available(): Boolean = engine.available()

    override fun glossary(locale: String): List<TranslationGlossaryTerm> = glossaryPort.findAll().filter { it.appliesTo(locale) }

    override fun translate(
        locale: String,
        koreanTitle: String,
        glossary: List<TranslationGlossaryTerm>,
    ): String? = engine.translate(EngineRequest(locale, mapOf(FIELD to koreanTitle), glossary))?.fields?.get(FIELD)

    companion object {
        private const val FIELD = "title"
    }
}
