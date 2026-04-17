package com.sleekydz86.idolglow.mbrd.ui

import com.sleekydz86.idolglow.mbrd.infrastructure.realtime.MbrdEditorLiveSyncService
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.stereotype.Controller

@Controller
class MbrdEditorRealtimeController(
    private val liveSyncService: MbrdEditorLiveSyncService,
) {
    @MessageMapping("/mbrd/editor/live")
    @SendTo("/topic/mbrd.editor.live")
    fun broadcast(message: MbrdEditorLiveSyncMessage): MbrdEditorLiveSyncMessage {
        val payload = liveSyncService.publish(message.toCommand())
        return MbrdEditorLiveSyncMessage.from(payload)
    }
}
