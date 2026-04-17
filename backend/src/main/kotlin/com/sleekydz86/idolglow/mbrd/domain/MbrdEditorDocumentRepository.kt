package com.sleekydz86.idolglow.mbrd.domain

import java.time.Instant

interface MbrdEditorDocumentRepository {
    fun findById(id: MbrdDocumentId): MbrdEditorDocument?
    fun findByUrlSlug(urlSlug: String): MbrdEditorDocument?
    fun findLatest(): MbrdEditorDocument?
    fun findLatestByStatus(status: MbrdDocumentPublicationStatus): MbrdEditorDocument?
    fun findPrevious(
        id: MbrdDocumentId,
        updatedAt: Instant,
        status: MbrdDocumentPublicationStatus,
    ): MbrdEditorDocument?

    fun findNext(
        id: MbrdDocumentId,
        updatedAt: Instant,
        status: MbrdDocumentPublicationStatus,
    ): MbrdEditorDocument?

    fun findPage(
        page: Int,
        size: Int,
        query: String,
        embeddingLiteral: String,
        status: MbrdDocumentPublicationStatus,
    ): List<MbrdEditorDocument>

    fun count(query: String, status: MbrdDocumentPublicationStatus): Long
    fun deleteById(id: MbrdDocumentId): Boolean
    fun save(document: MbrdEditorDocument): MbrdEditorDocument
}
