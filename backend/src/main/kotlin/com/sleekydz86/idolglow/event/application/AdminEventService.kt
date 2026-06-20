package com.sleekydz86.idolglow.event.application

import com.sleekydz86.idolglow.event.application.dto.AdminEventDetailResult
import com.sleekydz86.idolglow.event.application.dto.AdminEventPageResult
import com.sleekydz86.idolglow.event.application.dto.AdminEventSummaryResult
import com.sleekydz86.idolglow.event.application.dto.toAdminEventPageResult
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
    fun findEvents(
        page: Int,
        size: Int,
        query: String,
        status: String,
    ): AdminEventPageResult {
        val normalizedStatus = status.trim().lowercase()
        if (normalizedStatus == "all") {
            return findAllStatusEvents(page, size, query)
        }

        val normalizedPage = (page - 1).coerceAtLeast(0)
        return mbrdEditorBootstrapService
            .list(normalizedPage, size, query, status)
            .toAdminEventPageResult()
    }

    private fun findAllStatusEvents(
        page: Int,
        size: Int,
        query: String,
    ): AdminEventPageResult {
        val normalizedPage = page.coerceAtLeast(1)
        val normalizedSize = size.coerceIn(1, 50)
        val published = mbrdEditorBootstrapService.list(0, 1000, query, "published").content
        val drafts = mbrdEditorBootstrapService.list(0, 1000, query, "draft").content
        val merged =
            (published + drafts)
                .distinctBy { it.documentId }
                .sortedByDescending { it.updatedAt }
                .map(AdminEventSummaryResult::from)
        val totalElements = merged.size.toLong()
        val totalPages = if (totalElements == 0L) 1 else ((totalElements + normalizedSize - 1) / normalizedSize).toInt()
        val fromIndex = ((normalizedPage - 1) * normalizedSize).coerceAtMost(merged.size)
        val toIndex = (fromIndex + normalizedSize).coerceAtMost(merged.size)
        val pageItems = if (fromIndex >= toIndex) emptyList() else merged.subList(fromIndex, toIndex)
        return AdminEventPageResult(
            items = pageItems,
            page = normalizedPage,
            size = normalizedSize,
            totalElements = totalElements,
            totalPages = totalPages,
        )
    }

    @Transactional(readOnly = true)
    fun findEvent(documentId: String): AdminEventDetailResult =
        AdminEventDetailResult.from(mbrdEditorBootstrapService.loadDocument(documentId))

    fun upsertEvent(command: MbrdSaveEditorDraftCommand): AdminEventDetailResult =
        AdminEventDetailResult.from(mbrdEditorBootstrapService.save(command))

    fun deleteEvent(documentId: String) {
        mbrdEditorBootstrapService.deleteDocument(documentId)
    }
}
