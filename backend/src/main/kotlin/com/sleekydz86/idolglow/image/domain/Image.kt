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
        require(targetSortOrder >= 0) { "정렬 순서는 0 이상이어야 합니다." }
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
            require(aggregateId > 0) { "연결 대상 ID는 1 이상이어야 합니다." }
            require(content.isNotEmpty()) { "이미지 내용은 비어 있을 수 없습니다." }
            require(sortOrder >= 0) { "정렬 순서는 0 이상이어야 합니다." }

            val normalizedOriginal = originalFilename.trim()
            require(normalizedOriginal.isNotEmpty()) { "원본 파일명은 비어 있을 수 없습니다." }

            val generatedFilename = generateUniqueFilename(normalizedOriginal)
            val storedImage = imageStorage.store(generatedFilename.uniqueFilename, content)

            require(storedImage.url.isNotBlank()) { "이미지 URL은 비어 있을 수 없습니다." }
            require(storedImage.fileSize > 0) { "파일 크기는 0보다 커야 합니다." }

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
            require(normalizedOriginal.isNotEmpty()) { "원본 파일명은 비어 있을 수 없습니다." }
            val extension = extractExtension(normalizedOriginal)
            return GeneratedFilename(
                uniqueFilename = "${UUID.randomUUID()}.$extension",
                extension = extension
            )
        }

        private fun extractExtension(filename: String): String {
            val extension = filename.substringAfterLast('.', "").lowercase()
            require(extension.isNotEmpty()) { "파일명에는 확장자가 포함되어야 합니다." }
            return extension
        }
    }
}

data class GeneratedFilename(
    val uniqueFilename: String,
    val extension: String
)
