package com.sleekydz86.idolglow.event.ui.dto

import com.sleekydz86.idolglow.mbrd.application.MbrdEditorDocumentPagePayload
import com.sleekydz86.idolglow.mbrd.application.MbrdEditorDocumentSummaryPayload
import com.sleekydz86.idolglow.mbrd.application.MbrdEditorDraftPayload
import com.sleekydz86.idolglow.webzine.application.dto.AdminIssueImageUploadResponse
import java.time.Instant

data class AdminEventImageUploadResponse(
    val url: String,
    val objectKey: String,
    val contentType: String,
    val size: Long,
) {
    companion object {
        fun from(response: AdminIssueImageUploadResponse): AdminEventImageUploadResponse =
            AdminEventImageUploadResponse(
                url = response.url,
                objectKey = response.objectKey,
                contentType = response.contentType,
                size = response.size,
            )
    }
}
