package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventExternalIdPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventMasterPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSourceRefPort
import com.sleekydz86.idolglow.eventinfo.domain.EventMaster
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaChangeType
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaSnapshot
import com.sleekydz86.idolglow.eventinfo.domain.ExternalIdScheme
import com.sleekydz86.idolglow.ops.application.port.OpsChangeHandler
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class EventMasterMergeHandler(
    private val eventMasterPort: EventMasterPort,
    private val eventSourceRefPort: EventSourceRefPort,
    private val objectMapper: ObjectMapper,
    private val eventMetaOutboxRecorder: EventMetaOutboxRecorder,
    private val eventExternalIdPort: EventExternalIdPort,
) : OpsChangeHandler {
    override val type: String = TYPE

    override fun validate(
        target: String,
        payload: String,
    ) {
        val from = loadMaster(target)
        val into = loadMaster(intoGlobalId(payload, target))
        require(from.globalId != into.globalId) { "같은 글로벌 공연끼리는 병합할 수 없습니다. globalId=$target" }
    }

    override fun snapshot(target: String): String {
        val from = loadMaster(target)
        val moved = eventSourceRefPort.findByGlobalId(from.globalId).map { "${it.source}|${it.sourceId}" }
        val movedExternalIds =
            eventExternalIdPort.findByGlobalId(from.globalId).map { "${it.scheme.name}|${it.externalId}" }
        return objectMapper.writeValueAsString(EventMasterMergeSnapshot(from.globalId, moved, movedExternalIds))
    }

    override fun apply(
        target: String,
        payload: String,
    ) {
        val from = loadMaster(target)
        val into = loadMaster(intoGlobalId(payload, target))
        from.mergeInto(into)
        eventMetaOutboxRecorder.record(from, EventMetaChangeType.MERGED, setOf(EventMetaSnapshot.FIELD_STATUS))
        eventSourceRefPort.findByGlobalId(from.globalId).forEach {
            it.assignMatch(into.globalId, it.matchStatus, it.matchScore, it.matchedAt)
        }
        eventExternalIdPort.findByGlobalId(from.globalId).forEach { it.moveTo(into.globalId) }
    }

    override fun revert(
        target: String,
        beforeSnapshot: String,
    ) {
        val snapshot = readSnapshot(beforeSnapshot, target)
        val master = loadMaster(target)
        master.restoreActive()
        eventMetaOutboxRecorder.record(master, EventMetaChangeType.RESTORED, setOf(EventMetaSnapshot.FIELD_STATUS))
        snapshot.movedRefs.forEach { key ->
            val (source, sourceId) = key.split('|', limit = 2)
            eventSourceRefPort.findBySourceAndSourceId(source, sourceId)?.let {
                it.assignMatch(snapshot.globalId, it.matchStatus, it.matchScore, it.matchedAt)
            }
        }
        snapshot.movedExternalIds.forEach { key ->
            val (scheme, externalId) = key.split('|', limit = 2)
            eventExternalIdPort.find(ExternalIdScheme.parse(scheme), externalId)?.moveTo(snapshot.globalId)
        }
    }

    private fun loadMaster(globalId: String): EventMaster {
        require(globalId.isNotBlank()) { "병합할 글로벌 공연 ID는 비울 수 없습니다." }
        return eventMasterPort.findByGlobalId(globalId.trim())
            ?: throw IllegalArgumentException("글로벌 공연을 찾을 수 없습니다. globalId=$globalId")
    }

    private fun intoGlobalId(
        payload: String,
        target: String,
    ): String =
        runCatching { objectMapper.readTree(payload).path("intoGlobalId").asText("") }.getOrElse {
            throw IllegalArgumentException("병합 요청 본문을 읽을 수 없습니다. target=$target")
        }

    private fun readSnapshot(
        beforeSnapshot: String,
        target: String,
    ): EventMasterMergeSnapshot =
        runCatching { objectMapper.readValue(beforeSnapshot, EventMasterMergeSnapshot::class.java) }.getOrElse {
            throw IllegalArgumentException("적용 전 상태를 읽을 수 없습니다. target=$target")
        }

    companion object {
        const val TYPE: String = "EVENT_MASTER_MERGE"
    }
}

data class EventMasterMergeSnapshot(
    val globalId: String = "",
    val movedRefs: List<String> = emptyList(),
    val movedExternalIds: List<String> = emptyList(),
)
