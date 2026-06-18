package com.sleekydz86.idolglow.event.ui.dto

import com.sleekydz86.idolglow.mbrd.application.MbrdEditorDocumentPagePayload

fun MbrdEditorDocumentPagePayload.toAdminEventPageResponse(): AdminEventPageResponse =
    AdminEventPageResponse(
        items = content.map(AdminEventSummaryResponse::from),
        page = page + 1,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages,
    )
