package com.sleekydz86.idolglow.event.adapter.web.dto

import com.sleekydz86.idolglow.webzine.application.dto.AdminIssueImageUploadResponse

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
