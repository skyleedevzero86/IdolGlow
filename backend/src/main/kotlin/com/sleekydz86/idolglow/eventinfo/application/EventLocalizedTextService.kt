package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventMasterPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSourceRefPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventTranslationPort
import com.sleekydz86.idolglow.eventinfo.domain.EventLocale
import com.sleekydz86.idolglow.eventinfo.domain.EventLocalizedText
import com.sleekydz86.idolglow.eventinfo.domain.EventSourceRef
import com.sleekydz86.idolglow.eventinfo.domain.EventTextLookup
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslation
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslationFallback
import org.springframework.stereotype.Service

@Service
class EventLocalizedTextService(
    private val eventTranslationPort: EventTranslationPort,
    private val eventSourceRefPort: EventSourceRefPort,
    private val eventMasterPort: EventMasterPort,
) {
    fun resolve(
        items: List<EventTextLookup>,
        requested: String,
    ): List<EventLocalizedText> {
        if (items.isEmpty()) return emptyList()
        val refs = refsFor(items)
        val translations = translationsFor(refs.values)
        return items.map { resolveOne(it, requested, refs, translations) }
    }

    fun title(
        globalId: String,
        requested: String?,
    ): EventLocalizedText? {
        val master = eventMasterPort.findByGlobalId(globalId.trim()) ?: return null
        val picked =
            EventTranslationFallback.pick(
                requested ?: EventLocale.KOREAN,
                eventTranslationPort.findByGlobalId(master.globalId),
            )
        return picked?.let { EventLocalizedText(it.title, it.synopsis, it.locale) }
            ?: EventLocalizedText(master.title, null, EventLocale.KOREAN)
    }

    private fun refsFor(items: List<EventTextLookup>): Map<Pair<String, String>, EventSourceRef> {
        val wanted = items.map { it.source to it.sourceId }.toSet()
        return eventSourceRefPort
            .findBySourceInAndSourceIdIn(
                items.map { it.source }.distinct(),
                items.map { it.sourceId }.distinct(),
            )
            .filter { (it.source to it.sourceId) in wanted }
            .associateBy { it.source to it.sourceId }
    }

    private fun translationsFor(refs: Collection<EventSourceRef>): Map<String, List<EventTranslation>> {
        val globalIds = refs.mapNotNull { it.globalId }.distinct()
        if (globalIds.isEmpty()) return emptyMap()
        return eventTranslationPort.findByGlobalIdIn(globalIds).groupBy { it.globalId }
    }

    private fun resolveOne(
        item: EventTextLookup,
        requested: String,
        refs: Map<Pair<String, String>, EventSourceRef>,
        translations: Map<String, List<EventTranslation>>,
    ): EventLocalizedText {
        val fallback = EventLocalizedText(item.title, item.synopsis, EventLocale.KOREAN)
        val globalId = refs[item.source to item.sourceId]?.globalId
        val picked = globalId?.let { EventTranslationFallback.pick(requested, translations[it].orEmpty()) }
        return picked?.let { EventLocalizedText(it.title, it.synopsis, it.locale) } ?: fallback
    }
}
