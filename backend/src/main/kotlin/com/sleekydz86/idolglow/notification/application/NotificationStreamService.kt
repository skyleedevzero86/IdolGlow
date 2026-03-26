package com.sleekydz86.idolglow.notification.application

import com.sleekydz86.idolglow.notification.application.dto.NotificationResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class NotificationStreamService(
    private val notificationQueryService: NotificationQueryService,
    @Value("\${notification.sse.timeout-ms:1800000}")
    private val timeoutMs: Long,
) {

    private val emitters = ConcurrentHashMap<Long, ConcurrentHashMap<String, SseEmitter>>()

    fun subscribe(userId: Long): SseEmitter {
        val emitter = SseEmitter(timeoutMs)
        val emitterId = UUID.randomUUID().toString()
        val userEmitters = emitters.computeIfAbsent(userId) { ConcurrentHashMap() }
        userEmitters[emitterId] = emitter

        emitter.onCompletion { removeEmitter(userId, emitterId) }
        emitter.onTimeout { removeEmitter(userId, emitterId) }
        emitter.onError { removeEmitter(userId, emitterId) }

        sendInternal(emitter, "connected", mapOf("userId" to userId))
        notificationQueryService.findNotifications(userId)
            .take(20)
            .asReversed()
            .forEach { sendInternal(emitter, "notification", it) }

        return emitter
    }

    fun sendToUser(userId: Long, notification: NotificationResponse) {
        emitters[userId]
            ?.entries
            ?.toList()
            ?.forEach { (emitterId, emitter) ->
                try {
                    sendInternal(emitter, "notification", notification)
                } catch (_: IOException) {
                    removeEmitter(userId, emitterId)
                }
            }
    }

    private fun sendInternal(emitter: SseEmitter, eventName: String, data: Any) {
        emitter.send(
            SseEmitter.event()
                .name(eventName)
                .data(data)
        )
    }

    private fun removeEmitter(userId: Long, emitterId: String) {
        emitters[userId]?.remove(emitterId)
        if (emitters[userId].isNullOrEmpty()) {
            emitters.remove(userId)
        }
    }
}
