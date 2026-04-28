package com.sleekydz86.idolglow.mbrd.infrastructure.storage

import com.sleekydz86.idolglow.global.infrastructure.config.MinioStorageProperties
import com.sleekydz86.idolglow.mbrd.application.MbrdEditorImageContentPayload
import com.sleekydz86.idolglow.mbrd.application.MbrdEditorImageUploadPayload
import com.sleekydz86.idolglow.mbrd.infrastructure.config.MbrdEditorProperties
import com.sleekydz86.idolglow.mbrd.domain.MbrdEditorAsset
import com.sleekydz86.idolglow.mbrd.domain.MbrdEditorAssetRepository
import io.minio.BucketExistsArgs
import io.minio.GetObjectArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import java.util.UUID

@Service
@ConditionalOnBean(MinioClient::class)
class MbrdMinioEditorImageStorageService(
    private val minioClientProvider: ObjectProvider<MinioClient>,
    private val minioStorageProperties: MinioStorageProperties,
    private val mbrdEditorProperties: MbrdEditorProperties,
    private val assetRepository: MbrdEditorAssetRepository,
    private val clock: Clock,
) {
    private val minioClient: MinioClient
        get() = minioClientProvider.getObject()

    fun upload(image: MultipartFile): MbrdEditorImageUploadPayload {
        validate(image)
        ensureBucketExists()
        val assetId = UUID.randomUUID()
        val objectKey = buildObjectKey(image.originalFilename)
        val contentType = image.contentType ?: "application/octet-stream"
        image.inputStream.use { inputStream ->
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket())
                    .`object`(objectKey)
                    .stream(inputStream, image.size, -1)
                    .contentType(contentType)
                    .build(),
            )
        }
        assetRepository.save(
            MbrdEditorAsset(
                id = assetId,
                objectKey = objectKey,
                bucketName = bucket(),
                originalFileName = image.originalFilename ?: objectKey,
                contentType = contentType,
                fileSize = image.size,
                uploadedAt = clock.instant(),
            ),
        )
        val base = mbrdEditorProperties.imagePublicBasePath.trimEnd('/')
        return MbrdEditorImageUploadPayload(
            imageUrl = "$base/$assetId",
            originalFileName = image.originalFilename ?: objectKey,
            storedFileName = objectKey,
            size = image.size,
        )
    }

    fun load(assetId: UUID): MbrdEditorImageContentPayload {
        val asset = assetRepository.findById(assetId)
            ?: throw IllegalArgumentException("업로드한 이미지를 찾을 수 없습니다.")
        val stream = minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(asset.bucketName)
                .`object`(asset.objectKey)
                .build(),
        )
        return MbrdEditorImageContentPayload(
            stream = stream,
            contentType = asset.contentType,
            originalFileName = asset.originalFileName,
            size = asset.fileSize,
        )
    }

    private fun bucket(): String = minioStorageProperties.bucket.ifBlank { "idolglow" }

    private fun ensureBucketExists() {
        val bucket = bucket()
        val exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
        }
    }

    private fun buildObjectKey(originalFileName: String?): String {
        val currentDate = LocalDate.now(clock.withZone(ZoneOffset.UTC))
        val ext = extractExtension(originalFileName)
        return "mbrd-editor-images/%d/%02d/%02d/%s%s".format(
            currentDate.year,
            currentDate.monthValue,
            currentDate.dayOfMonth,
            UUID.randomUUID(),
            ext,
        )
    }

    private fun validate(image: MultipartFile) {
        if (image.isEmpty) {
            throw IllegalArgumentException("업로드할 이미지 파일을 선택해주세요.")
        }
        val contentType = (image.contentType ?: "").lowercase(Locale.ROOT)
        if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw IllegalArgumentException("PNG, JPG, GIF, WEBP, SVG 이미지만 업로드할 수 있습니다.")
        }
    }

    private fun extractExtension(fileName: String?): String {
        val safeName = fileName ?: ""
        val extensionIndex = safeName.lastIndexOf('.')
        return if (extensionIndex < 0) "" else safeName.substring(extensionIndex)
    }

    companion object {
        private val ALLOWED_IMAGE_TYPES = setOf(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/gif",
            "image/webp",
            "image/svg+xml",
        )
    }
}
