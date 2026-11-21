package com.sleekydz86.idolglow.translation.application

import com.sleekydz86.idolglow.translation.application.port.out.ContentTranslationPort
import com.sleekydz86.idolglow.translation.domain.ContentTranslation
import com.sleekydz86.idolglow.translation.domain.SourceContent
import com.sleekydz86.idolglow.translation.domain.TranslationContentType
import com.sleekydz86.idolglow.translation.domain.TranslationLocales
import com.sleekydz86.idolglow.translation.domain.TranslationStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class TranslationEnqueueService(
    private val contentTranslationPort: ContentTranslationPort,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun ensure(
        source: SourceContent,
        priority: Int,
    ): Int {
        if (source.fields.isEmpty()) return 0
        val hash = source.hash()
        val existing = contentTranslationPort.findByTarget(source.type, source.key).associateBy { it.locale }
        var changed = 0
        TranslationLocales.TARGETS.forEach { locale ->
            val row = existing[locale]
            when {
                row == null -> {
                    contentTranslationPort.save(
                        ContentTranslation.pending(source.type, source.key, locale, hash, priority),
                    )
                    changed += 1
                }
                row.sourceHash != hash -> {
                    row.resetFor(hash, priority)
                    contentTranslationPort.save(row)
                    changed += 1
                }
                priority > row.priority -> {
                    row.raisePriority(priority)
                    contentTranslationPort.save(row)
                }
            }
        }
        return changed
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun retire(
        type: TranslationContentType,
        key: String,
    ): Int {
        val rows =
            contentTranslationPort
                .findByTarget(type, key)
                .filter { it.status != TranslationStatus.SKIPPED }
        rows.forEach { row ->
            row.skip("source_unavailable")
            contentTranslationPort.save(row)
        }
        return rows.size
    }
}
