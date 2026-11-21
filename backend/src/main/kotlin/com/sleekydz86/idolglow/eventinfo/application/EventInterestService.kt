package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventInterestPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventMasterPort
import com.sleekydz86.idolglow.eventinfo.domain.EventInterest
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EventInterestService(
    private val eventInterestPort: EventInterestPort,
    private val eventMasterPort: EventMasterPort,
) {
    @Transactional
    fun register(
        userId: Long,
        globalId: String,
    ): EventInterestView {
        val master =
            eventMasterPort.findByGlobalId(globalId.trim())
                ?: throw EntityNotFoundException("공연을 찾을 수 없습니다. globalId=$globalId")
        val interest =
            eventInterestPort.find(userId, master.globalId)
                ?: eventInterestPort.save(EventInterest(userId = userId, globalId = master.globalId))
        return EventInterestView(interest.globalId, master.title)
    }

    @Transactional
    fun unregister(
        userId: Long,
        globalId: String,
    ) {
        eventInterestPort.find(userId, globalId.trim())?.let(eventInterestPort::delete)
    }

    @Transactional(readOnly = true)
    fun list(userId: Long): List<EventInterestView> =
        eventInterestPort.findByUserId(userId).map { interest ->
            EventInterestView(interest.globalId, eventMasterPort.findByGlobalId(interest.globalId)?.title.orEmpty())
        }
}

data class EventInterestView(
    val globalId: String,
    val title: String,
)
