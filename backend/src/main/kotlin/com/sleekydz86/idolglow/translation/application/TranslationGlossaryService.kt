package com.sleekydz86.idolglow.translation.application

import com.sleekydz86.idolglow.translation.application.dto.GlossaryTermResult
import com.sleekydz86.idolglow.translation.application.port.out.TranslationGlossaryPort
import com.sleekydz86.idolglow.translation.domain.GlossaryMode
import com.sleekydz86.idolglow.translation.domain.TranslationGlossaryTerm
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TranslationGlossaryService(
    private val glossaryPort: TranslationGlossaryPort,
) {
    @Transactional(readOnly = true)
    fun list(): List<GlossaryTermResult> = glossaryPort.findAll().map(::toResult)

    @Transactional
    fun add(
        sourceTerm: String,
        mode: GlossaryMode,
        locale: String?,
        targetTerm: String?,
    ): GlossaryTermResult {
        val term =
            when (mode) {
                GlossaryMode.KEEP -> TranslationGlossaryTerm.keep(sourceTerm)
                GlossaryMode.FIXED ->
                    TranslationGlossaryTerm.fixed(
                        sourceTerm,
                        requireNotNull(locale) { "고정 번역어는 언어를 입력해야 합니다." },
                        requireNotNull(targetTerm) { "고정 번역어를 입력해야 합니다." },
                    )
            }
        return toResult(glossaryPort.save(term))
    }

    @Transactional
    fun delete(id: Long) {
        require(glossaryPort.delete(id)) { "용어를 찾을 수 없습니다. id=$id" }
    }

    private fun toResult(term: TranslationGlossaryTerm): GlossaryTermResult =
        GlossaryTermResult(term.id, term.sourceTerm, term.locale, term.targetTerm, term.mode.name)
}
