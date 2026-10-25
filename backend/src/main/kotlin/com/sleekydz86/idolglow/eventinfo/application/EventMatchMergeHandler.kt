package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventMasterPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSourceRefPort
import com.sleekydz86.idolglow.eventinfo.domain.EventMatchStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventPerformanceMatcher
import com.sleekydz86.idolglow.eventinfo.domain.EventSourceRef
import com.sleekydz86.idolglow.ops.application.port.OpsChangeHandler
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime

@Component
class EventMatchMergeHandler(
    private val eventSourceRefPort: EventSourceRefPort,
    private val eventMasterPort: EventMasterPort,
    private val objectMapper: ObjectMapper,
) : OpsChangeHandler {
    override val type: String = TYPE

    override fun validate(
        target: String,
        payload: String,
    ) {
        loadRef(target)
        requireMaster(readPayload(payload, target).globalId)
    }

    override fun snapshot(target: String): String {
        val ref = loadRef(target)
        return objectMapper.writeValueAsString(
            EventMatchSnapshot(
                globalId = ref.globalId,
                matchStatus = ref.matchStatus.name,
                matchScore = ref.matchScore,
                matchedAt = ref.matchedAt?.toString(),
            ),
        )
    }

    override fun apply(
        target: String,
        payload: String,
    ) {
        val globalId = readPayload(payload, target).globalId
        requireMaster(globalId)
        loadRef(target).assignMatch(
            globalId,
            EventMatchStatus.CONFIRMED,
            EventPerformanceMatcher.CONFIRMED_SCORE,
            LocalDateTime.now(),
        )
    }

    override fun revert(
        target: String,
        beforeSnapshot: String,
    ) {
        val snapshot = readSnapshot(beforeSnapshot, target)
        val status =
            runCatching { EventMatchStatus.valueOf(snapshot.matchStatus) }.getOrElse {
                throw IllegalArgumentException("매칭 상태를 복원할 수 없습니다. status=${snapshot.matchStatus} target=$target")
            }
        loadRef(target).assignMatch(
            snapshot.globalId,
            status,
            snapshot.matchScore,
            snapshot.matchedAt?.let(LocalDateTime::parse),
        )
    }

    private fun loadRef(target: String): EventSourceRef {
        val (source, sourceId) = parseTarget(target)
        return eventSourceRefPort.findBySourceAndSourceId(source, sourceId)
            ?: throw IllegalArgumentException("공연 공급처 원본을 찾을 수 없습니다. target=$target")
    }

    private fun requireMaster(globalId: String) {
        require(globalId.isNotBlank()) { "병합할 글로벌 공연 ID는 비울 수 없습니다." }
        require(eventMasterPort.findByGlobalId(globalId) != null) {
            "글로벌 공연을 찾을 수 없습니다. globalId=$globalId"
        }
    }

    private fun readPayload(
        payload: String,
        target: String,
    ): EventMatchMergePayload {
        val globalId = globalIdOf(payload, target)
        require(globalId.isNotBlank()) { "병합 요청 본문을 읽을 수 없습니다. target=$target" }
        return EventMatchMergePayload(globalId)
    }

    private fun globalIdOf(
        payload: String,
        target: String,
    ): String =
        runCatching { objectMapper.readTree(payload).path("globalId").asText("") }.getOrElse {
            throw IllegalArgumentException("병합 요청 본문을 읽을 수 없습니다. target=$target")
        }

    private fun readSnapshot(
        beforeSnapshot: String,
        target: String,
    ): EventMatchSnapshot =
        runCatching { objectMapper.readValue(beforeSnapshot, EventMatchSnapshot::class.java) }.getOrElse {
            throw IllegalArgumentException("적용 전 상태를 읽을 수 없습니다. target=$target")
        }

    private fun parseTarget(target: String): Pair<String, String> {
        val parts = target.split('|', limit = 2)
        require(parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
            "공연 대상 형식은 공급처|공급처ID 입니다. target=$target"
        }
        return parts[0] to parts[1]
    }

    companion object {
        const val TYPE: String = "EVENT_MATCH_MERGE"
    }
}

data class EventMatchMergePayload(
    val globalId: String = "",
    val reason: String = "",
)

data class EventMatchSnapshot(
    val globalId: String? = null,
    val matchStatus: String = "",
    val matchScore: Int = 0,
    val matchedAt: String? = null,
)
