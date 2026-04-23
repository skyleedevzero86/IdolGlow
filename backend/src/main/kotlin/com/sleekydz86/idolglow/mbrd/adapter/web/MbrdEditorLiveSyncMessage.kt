package com.sleekydz86.idolglow.mbrd.ui

import com.sleekydz86.idolglow.mbrd.application.MbrdEditorLiveSyncCommand
import com.sleekydz86.idolglow.mbrd.application.MbrdEditorLiveSyncPayload

data class MbrdEditorLiveSyncMessage(
    val sessionId: String?,
    val documentId: String?,
    val title: String?,
    val author: String?,
    val markdown: String?,
    val tags: List<String>?,
    val status: String?,
    val updatedAt: String?,
) {
    fun toCommand(): MbrdEditorLiveSyncCommand =
        MbrdEditorLiveSyncCommand(sessionId, documentId, title, author, markdown, tags, status)

    companion object {
        fun from(view: MbrdEditorLiveSyncPayload): MbrdEditorLiveSyncMessage =
            MbrdEditorLiveSyncMessage(
                sessionId = view.sessionId,
                documentId = view.documentId,
                title = view.title,
                author = view.author,
                markdown = view.markdown,
                tags = view.tags,
                status = view.status,
                updatedAt = view.updatedAt.toString(),
            )
    }
}
