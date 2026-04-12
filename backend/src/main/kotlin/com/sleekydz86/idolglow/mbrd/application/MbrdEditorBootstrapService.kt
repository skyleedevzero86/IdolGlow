package com.sleekydz86.idolglow.mbrd.application

import com.sleekydz86.idolglow.mbrd.domain.MbrdDocumentId
import com.sleekydz86.idolglow.mbrd.domain.MbrdDocumentPublicationStatus
import com.sleekydz86.idolglow.mbrd.domain.MbrdEditorDocument
import com.sleekydz86.idolglow.mbrd.domain.MbrdEditorDocumentRepository
import com.sleekydz86.idolglow.mbrd.infrastructure.MbrdEditorEmbeddingEncoder
import com.sleekydz86.idolglow.mbrd.infrastructure.MbrdEditorSeedFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.Locale
import kotlin.math.ceil

@Service
@Transactional
class MbrdEditorBootstrapService(
    private val repository: MbrdEditorDocumentRepository,
    private val seedFactory: MbrdEditorSeedFactory,
    private val embeddingEncoder: MbrdEditorEmbeddingEncoder,
    private val clock: Clock,
) {
    fun loadBootstrap(): MbrdEditorBootstrapPayload {
        val document = repository.findLatestByStatus(MbrdDocumentPublicationStatus.PUBLISHED)
            ?: repository.findLatest()
            ?: repository.save(seedFactory.create(clock))
        return MbrdEditorBootstrapPayload(
            draft = toDraftPayload(document),
            suggestedTags = SUGGESTED_TAGS,
            referenceImageUrl = "/reference-editor-flow.svg",
        )
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = ["mbrd-editor-document-by-id"], key = "#documentId")
    fun loadDocument(documentId: String): MbrdEditorDraftPayload {
        val doc = repository.findById(MbrdDocumentId.from(documentId))
            ?: throw IllegalArgumentException("존재하지 않는 문서입니다.")
        return toDraftPayload(doc)
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = ["mbrd-editor-document-by-slug"], key = "#urlSlug")
    fun loadPublishedDocumentByUrlSlug(urlSlug: String): MbrdEditorDraftPayload {
        val doc = repository.findByUrlSlug(urlSlug)
            ?: throw IllegalArgumentException("존재하지 않는 문서입니다.")
        return toDraftPayload(doc)
    }

    @Transactional(readOnly = true)
    @Cacheable(
        cacheNames = ["mbrd-editor-document-pages"],
        key = "#status + ':' + #query + ':' + #page + ':' + #size",
    )
    fun list(page: Int, size: Int, query: String, status: String): MbrdEditorDocumentPagePayload {
        val normalizedPage = page.coerceAtLeast(0)
        val normalizedSize = size.coerceIn(1, 25)
        val normalizedQuery = normalizeQuery(query)
        val statusFilter = MbrdDocumentPublicationStatus.fromApiValue(status)
        val queryEmbedding = embeddingEncoder.encode(normalizedQuery)
        val totalElements = repository.count(normalizedQuery, statusFilter)
        val totalPages = if (totalElements == 0L) {
            1
        } else {
            ceil(totalElements.toDouble() / normalizedSize).toInt()
        }
        val content = repository.findPage(
            normalizedPage,
            normalizedSize,
            normalizedQuery,
            queryEmbedding,
            statusFilter,
        ).map { toSummaryPayload(it) }
        return MbrdEditorDocumentPagePayload(
            content = content,
            page = normalizedPage,
            size = normalizedSize,
            totalElements = totalElements,
            totalPages = totalPages,
            first = normalizedPage == 0,
            last = normalizedPage >= totalPages - 1,
            query = normalizedQuery,
            statusFilter = statusFilter.toApiValue(),
        )
    }

    @Transactional
    @CacheEvict(
        cacheNames = ["mbrd-editor-document-by-id", "mbrd-editor-document-by-slug", "mbrd-editor-document-pages"],
        allEntries = true,
    )
    fun save(command: MbrdSaveEditorDraftCommand): MbrdEditorDraftPayload {
        val documentId = if (command.documentId.isNullOrBlank()) {
            MbrdDocumentId.newId()
        } else {
            MbrdDocumentId.from(command.documentId)
        }
        val existing = repository.findById(documentId)
        val publicationStatus = resolvePublicationStatus(command.status, existing)
        val normalizedTags = (command.tags ?: emptyList())
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        val urlSlug = resolveUrlSlug(command.urlSlug, command.title, documentId, publicationStatus)
        val introduction = normalizeOptional(command.introduction)
        val thumbnailImageUrl = normalizeOptional(command.thumbnailImageUrl)
        val saved = if (existing == null) {
            repository.save(
                MbrdEditorDocument.create(
                    id = documentId,
                    title = command.title,
                    author = command.author,
                    markdown = command.markdown,
                    tags = normalizedTags,
                    urlSlug = urlSlug,
                    introduction = introduction,
                    thumbnailImageUrl = thumbnailImageUrl,
                    publicationStatus = publicationStatus,
                    clock = clock,
                ),
            )
        } else {
            repository.save(
                existing.refresh(
                    title = command.title,
                    author = command.author,
                    markdown = command.markdown,
                    tags = normalizedTags,
                    urlSlug = urlSlug,
                    introduction = introduction,
                    thumbnailImageUrl = thumbnailImageUrl,
                    publicationStatus = publicationStatus,
                    clock = clock,
                ),
            )
        }
        return toDraftPayload(saved)
    }

    @Transactional
    @CacheEvict(
        cacheNames = ["mbrd-editor-document-by-id", "mbrd-editor-document-by-slug", "mbrd-editor-document-pages"],
        allEntries = true,
    )
    fun deleteDocument(documentId: String) {
        val deleted = repository.deleteById(MbrdDocumentId.from(documentId))
        if (!deleted) {
            throw IllegalArgumentException("존재하지 않는 문서입니다.")
        }
    }

    private fun resolvePublicationStatus(
        requestedStatus: String?,
        existing: MbrdEditorDocument?,
    ): MbrdDocumentPublicationStatus {
        if (requestedStatus.isNullOrBlank()) {
            return existing?.publicationStatus ?: MbrdDocumentPublicationStatus.PUBLISHED
        }
        return MbrdDocumentPublicationStatus.fromApiValue(requestedStatus)
    }

    private fun toDraftPayload(document: MbrdEditorDocument): MbrdEditorDraftPayload {
        val previous = repository.findPrevious(document.id, document.updatedAt, document.publicationStatus)
            ?.let { toSummaryPayload(it) }
        val next = repository.findNext(document.id, document.updatedAt, document.publicationStatus)
            ?.let { toSummaryPayload(it) }
        return MbrdEditorDraftPayload(
            documentId = document.id.asString(),
            title = document.title,
            author = document.author,
            markdown = document.markdown,
            tags = document.tags,
            urlSlug = document.urlSlug,
            introduction = document.introduction,
            thumbnailImageUrl = document.thumbnailImageUrl,
            status = document.publicationStatus.toApiValue(),
            updatedAt = document.updatedAt,
            previousDocument = previous,
            nextDocument = next,
        )
    }

    private fun toSummaryPayload(document: MbrdEditorDocument): MbrdEditorDocumentSummaryPayload =
        MbrdEditorDocumentSummaryPayload(
            documentId = document.id.asString(),
            title = document.title,
            author = document.author,
            introduction = document.introduction,
            thumbnailImageUrl = document.thumbnailImageUrl,
            tags = document.tags,
            status = document.publicationStatus.toApiValue(),
            updatedAt = document.updatedAt,
        )

    private fun resolveUrlSlug(
        requestedUrlSlug: String?,
        title: String,
        documentId: MbrdDocumentId,
        publicationStatus: MbrdDocumentPublicationStatus,
    ): String? {
        var source = requestedUrlSlug
        if (source.isNullOrBlank() && publicationStatus == MbrdDocumentPublicationStatus.PUBLISHED) {
            source = title
        }
        val normalized = normalizeSlug(source)
        if (normalized != null) return normalized
        if (publicationStatus != MbrdDocumentPublicationStatus.PUBLISHED) return null
        return fallbackSlug(documentId)
    }

    private fun normalizeSlug(value: String?): String? {
        val normalized = normalizeOptional(value) ?: return null
        val slug = normalized.lowercase(Locale.ROOT)
            .replace("[^\\p{L}\\p{N}\\s-]".toRegex(), " ")
            .trim()
            .replace("[-\\s]+".toRegex(), "-")
            .replace("^-+|-+$".toRegex(), "")
        return if (slug.isBlank()) null else slug
    }

    private fun fallbackSlug(documentId: MbrdDocumentId): String {
        val raw = documentId.asString().replace("-", "")
        val length = raw.length.coerceAtMost(8)
        return "post-" + raw.substring(0, length).lowercase(Locale.ROOT)
    }

    private fun normalizeOptional(value: String?): String? {
        if (value == null) return null
        val t = value.trim()
        return if (t.isEmpty()) null else t
    }

    private fun normalizeQuery(query: String?): String = query?.trim() ?: ""

    companion object {
        private val SUGGESTED_TAGS = listOf(
            "위키스타일",
            "다시읽기",
            "마크다운",
            "리액트",
            "타입스크립트",
            "Spring Boot 4",
        )
    }
}
