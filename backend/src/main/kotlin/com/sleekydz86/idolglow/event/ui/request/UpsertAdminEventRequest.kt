package com.sleekydz86.idolglow.event.ui.request

import com.sleekydz86.idolglow.mbrd.application.MbrdSaveEditorDraftCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpsertAdminEventRequest(
    val documentId: String?,
    @field:NotBlank
    @field:Size(max = 120)
    val title: String,
    @field:NotBlank
    @field:Size(max = 60)
    val author: String,
    @field:NotBlank
    val markdown: String,
    val tags: List<String>?,
    @field:Size(max = 180)
    val urlSlug: String?,
    @field:Size(max = 8000)
    val introduction: String?,
    @field:Size(max = 500)
    val thumbnailImageUrl: String?,
    val status: String?,
) {
    fun toCommand(): MbrdSaveEditorDraftCommand =
        MbrdSaveEditorDraftCommand(
            documentId = documentId,
            title = title,
            author = author,
            markdown = markdown,
            tags = tags ?: emptyList(),
            urlSlug = urlSlug,
            introduction = introduction,
            thumbnailImageUrl = thumbnailImageUrl,
            status = status,
        )
}
