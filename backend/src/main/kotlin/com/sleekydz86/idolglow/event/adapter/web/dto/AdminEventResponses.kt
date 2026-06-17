package com.sleekydz86.idolglow.event.ui.dto

import com.sleekydz86.idolglow.mbrd.application.MbrdEditorDocumentPagePayload
import com.sleekydz86.idolglow.mbrd.application.MbrdEditorDocumentSummaryPayload
import com.sleekydz86.idolglow.mbrd.application.MbrdEditorDraftPayload
import com.sleekydz86.idolglow.webzine.application.dto.AdminIssueImageUploadResponse
import java.time.Instant

fun MbrdEditorDocumentPagePayload.toAdminEventPageResponse(): AdminEventPageResponse =
    AdminEventPageResponse(
        items = content.map(AdminEventSummaryResponse::from),
        page = page + 1,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages,
    )
