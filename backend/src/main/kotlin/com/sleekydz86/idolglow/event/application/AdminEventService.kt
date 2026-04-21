package com.sleekydz86.idolglow.event.application

import com.sleekydz86.idolglow.event.ui.dto.AdminEventDetailResponse
import com.sleekydz86.idolglow.event.ui.dto.AdminEventPageResponse
import com.sleekydz86.idolglow.event.ui.dto.toAdminEventPageResponse
import com.sleekydz86.idolglow.mbrd.application.MbrdEditorBootstrapService
import com.sleekydz86.idolglow.mbrd.application.MbrdSaveEditorDraftCommand
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AdminEventService(
    private val mbrdEditorBootstrapService: MbrdEditorBootstrapService,
) {
    @Transactional(readOnly = true)
    fun findEvents(page: Int, size: Int, query: String, status: String): AdminEventPageResponse {
        val normalizedPage = (page - 1).coerceAtLeast(0)
        return mbrdEditorBootstrapService
            .list(normalizedPage, size, query, status)
            .toAdminEventPageResponse()
    }

    @Transactional(readOnly = true)
    fun findEvent(documentId: String): AdminEventDetailResponse =
        AdminEventDetailResponse.from(mbrdEditorBootstrapService.loadDocument(documentId))

    fun upsertEvent(command: MbrdSaveEditorDraftCommand): AdminEventDetailResponse =
        AdminEventDetailResponse.from(mbrdEditorBootstrapService.save(command))

    fun deleteEvent(documentId: String) {
        mbrdEditorBootstrapService.deleteDocument(documentId)
    }
}
