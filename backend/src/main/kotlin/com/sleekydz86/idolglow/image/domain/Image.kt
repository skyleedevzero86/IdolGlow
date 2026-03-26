package com.sleekydz86.idolglow.image.domain

import com.sleekydz86.idolglow.global.BaseEntity
import com.sleekydz86.idolglow.image.domain.domainservice.ImageStorage
import com.sleekydz86.idolglow.image.domain.vo.ImageAggregateType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "images")
class Image(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Enumerated(EnumType.STRING)
    @Column(name = "aggregate_type", nullable = false, length = 30)
    val aggregateType: ImageAggregateType,

    @Column(name = "aggregate_id", nullable = false)
    val aggregateId: Long,

    @Column(name = "original_filename", nullable = false, length = 255)
    val originalFilename: String,

    @Column(name = "unique_filename", nullable = false, length = 255)
    val uniqueFilename: String,

    @Column(name = "extension", nullable = false, length = 20)
    val extension: String,

    @Column(name = "file_size", nullable = false)
    val fileSize: Long,

    @Column(name = "url", nullable = false, length = 500)
    val url: String,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,
) : BaseEntity() {

    fun changeSortOrder(targetSortOrder: Int): Image {
        require(targetSortOrder >= 0) { "sortOrder must be zero or positive." }
        sortOrder = targetSortOrder
        return this
    }

    companion object {
        fun createAndStore(
            aggregateType: ImageAggregateType,
            aggregateId: Long,
            originalFilename: String,
            content: ByteArray,
            sortOrder: Int = 0,
            imageStorage: ImageStorage,
        ): Image {
            require(aggregateId > 0) { "aggregateId must be positive." }
            require(content.isNotEmpty()) { "content must not be empty." }
            require(sortOrder >= 0) { "sortOrder must be zero or positive." }

            val normalizedOriginal = originalFilename.trim()
            require(normalizedOriginal.isNotEmpty()) { "originalFilename must not be blank." }

            val generatedFilename = generateUniqueFilename(normalizedOriginal)
            val storedImage = imageStorage.store(generatedFilename.uniqueFilename, content)

            require(storedImage.url.isNotBlank()) { "url must not be blank." }
            require(storedImage.fileSize > 0) { "fileSize must be greater than zero." }

            return Image(
                aggregateType = aggregateType,
                aggregateId = aggregateId,
                originalFilename = normalizedOriginal,
                uniqueFilename = generatedFilename.uniqueFilename,
                extension = generatedFilename.extension,
                fileSize = storedImage.fileSize,
                url = storedImage.url,
                sortOrder = sortOrder,
            )
        }

        fun generateUniqueFilename(originalFilename: String): GeneratedFilename {
            val normalizedOriginal = originalFilename.trim()
            require(normalizedOriginal.isNotEmpty()) { "originalFilename must not be blank." }
            val extension = extractExtension(normalizedOriginal)
            return GeneratedFilename(
                uniqueFilename = "${UUID.randomUUID()}.$extension",
                extension = extension
            )
        }

        private fun extractExtension(filename: String): String {
            val extension = filename.substringAfterLast('.', "").lowercase()
            require(extension.isNotEmpty()) { "filename must contain an extension." }
            return extension
        }
    }
}

data class GeneratedFilename(
    val uniqueFilename: String,
    val extension: String
)
