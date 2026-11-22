package com.sleekydz86.idolglow.comment.application

import com.sleekydz86.idolglow.comment.application.event.ContentCommentCreatedEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ContentCommentNotificationListener(
    private val contentCommentNotificationTxService: ContentCommentNotificationTxService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: ContentCommentCreatedEvent) {
        runCatching { contentCommentNotificationTxService.notify(event) }
            .onFailure { log.warn("댓글 알림 생성 실패 commentId={} error={}", event.commentId, it.message) }
    }
}
