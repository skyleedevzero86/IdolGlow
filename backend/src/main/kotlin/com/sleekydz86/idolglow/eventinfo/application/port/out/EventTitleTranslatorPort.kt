package com.sleekydz86.idolglow.eventinfo.application.port.out

import com.sleekydz86.idolglow.translation.domain.TranslationGlossaryTerm

interface EventTitleTranslatorPort {
    fun available(): Boolean

    fun glossary(locale: String): List<TranslationGlossaryTerm>

    fun translate(
        locale: String,
        koreanTitle: String,
        glossary: List<TranslationGlossaryTerm>,
    ): String?
}
