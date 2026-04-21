package com.sleekydz86.idolglow.mbrd.domain

import java.time.Clock
import java.time.Instant

private fun mbrdNormalizeOptional(value: String?): String? {
    if (value == null) return null
    val t = value.trim()
    return if (t.isEmpty()) null else t
}

data class MbrdEditorDocument(
    val id: MbrdDocumentId,
    val title: String,
    val author: String,
    val markdown: String,
    val tags: List<String>,
    val urlSlug: String?,
    val introduction: String?,
    val thumbnailImageUrl: String?,
    val publicationStatus: MbrdDocumentPublicationStatus,
    val updatedAt: Instant,
    val viewCount: Long = 0L,
) {
    init {
        require(title.isNotBlank())
        require(author.isNotBlank())
        require(markdown.isNotBlank())
        require(viewCount >= 0)
    }

    companion object {
        fun create(
            id: MbrdDocumentId,
            title: String,
            author: String,
            markdown: String,
            tags: List<String>,
            urlSlug: String?,
            introduction: String?,
            thumbnailImageUrl: String?,
            publicationStatus: MbrdDocumentPublicationStatus,
            clock: Clock,
        ): MbrdEditorDocument =
            MbrdEditorDocument(
                id = id,
                title = title,
                author = author,
                markdown = markdown,
                tags = tags.toList(),
                urlSlug = mbrdNormalizeOptional(urlSlug),
                introduction = mbrdNormalizeOptional(introduction),
                thumbnailImageUrl = mbrdNormalizeOptional(thumbnailImageUrl),
                publicationStatus = publicationStatus,
                updatedAt = clock.instant(),
                viewCount = 0L,
            )
    }

    fun refresh(
        title: String,
        author: String,
        markdown: String,
        tags: List<String>,
        urlSlug: String?,
        introduction: String?,
        thumbnailImageUrl: String?,
        publicationStatus: MbrdDocumentPublicationStatus,
        clock: Clock,
    ): MbrdEditorDocument =
        MbrdEditorDocument(
            id = id,
            title = title,
            author = author,
            markdown = markdown,
            tags = tags.toList(),
            urlSlug = mbrdNormalizeOptional(urlSlug),
            introduction = mbrdNormalizeOptional(introduction),
            thumbnailImageUrl = mbrdNormalizeOptional(thumbnailImageUrl),
            publicationStatus = publicationStatus,
            updatedAt = clock.instant(),
            viewCount = this.viewCount,
        )
}
