package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventExternalIdPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventMasterPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSourceRefPort
import com.sleekydz86.idolglow.eventinfo.domain.EventExternalId
import com.sleekydz86.idolglow.eventinfo.domain.ExternalIdScheme
import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.eventinfo.EventExternalIdExceptionType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class EventExternalIdService(
    private val eventExternalIdPort: EventExternalIdPort,
    private val eventSourceRefPort: EventSourceRefPort,
    private val eventMasterPort: EventMasterPort,
) {
    @Transactional(readOnly = true)
    fun idsOf(globalIds: Collection<String>): Map<String, List<ExternalIdView>> {
        if (globalIds.isEmpty()) return emptyMap()
        val fromSources =
            eventSourceRefPort.findByGlobalIdIn(globalIds).mapNotNull { ref ->
                val globalId = ref.confirmedGlobalId() ?: return@mapNotNull null
                val scheme = ExternalIdScheme.ofSource(ref.source) ?: return@mapNotNull null
                globalId to ExternalIdView(scheme.name, ref.sourceId)
            }
        val manual =
            eventExternalIdPort.findByGlobalIdIn(globalIds).map {
                it.globalId to ExternalIdView(it.scheme.name, it.externalId)
            }
        return (fromSources + manual)
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, ids) -> ids.distinct().sortedWith(compareBy({ it.scheme }, { it.id })) }
    }

    @Transactional(readOnly = true)
    fun resolve(
        scheme: ExternalIdScheme,
        externalId: String,
    ): String? {
        val value = externalId.trim()
        if (scheme.manual) return eventExternalIdPort.find(scheme, value)?.globalId
        return eventSourceRefPort
            .findBySourceInAndSourceIdIn(scheme.sources, listOf(value))
            .firstNotNullOfOrNull { it.confirmedGlobalId() }
    }

    @Transactional
    fun attach(
        globalId: String,
        scheme: ExternalIdScheme,
        externalId: String,
    ): ExternalIdView {
        val master =
            eventMasterPort.findByGlobalId(globalId.trim())
                ?: throw CustomException(EventExternalIdExceptionType.EVENT_NOT_FOUND)
        val candidate = EventExternalId.manual(master.globalId, scheme, externalId)
        val existing = eventExternalIdPort.find(scheme, candidate.externalId)
        if (existing != null && existing.globalId != master.globalId) {
            throw CustomException(EventExternalIdExceptionType.ALREADY_LINKED)
        }
        val saved = existing ?: eventExternalIdPort.save(candidate)
        master.markExternalIdChanged(LocalDateTime.now())
        return ExternalIdView(saved.scheme.name, saved.externalId)
    }

    @Transactional
    fun detach(
        globalId: String,
        scheme: ExternalIdScheme,
        externalId: String,
    ) {
        val linked =
            eventExternalIdPort
                .find(scheme, externalId.trim())
                ?.takeIf { it.globalId == globalId.trim() }
                ?: throw CustomException(EventExternalIdExceptionType.NOT_LINKED)
        eventExternalIdPort.delete(linked)
        eventMasterPort.findByGlobalId(linked.globalId)?.markExternalIdChanged(LocalDateTime.now())
    }
}

data class ExternalIdView(
    val scheme: String,
    val id: String,
)
