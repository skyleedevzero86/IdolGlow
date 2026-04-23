package com.sleekydz86.idolglow.sitecontent.application

import com.sleekydz86.idolglow.global.infrastructure.config.MinioStorageProperties
import com.sleekydz86.idolglow.sitecontent.application.dto.SiteContentAssetPayload
import io.minio.GetObjectArgs
import io.minio.MinioClient
import jakarta.persistence.EntityNotFoundException
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Service
class SiteContentAssetQueryService(
    private val minioStorageProperties: MinioStorageProperties,
    private val minioClientProvider: ObjectProvider<MinioClient>,
    @Value("\${app.storage.local.base-path:}") private val localBasePath: String,
) {
    fun readImage(objectKey: String): SiteContentAssetPayload {
        val normalizedObjectKey = normalizeObjectKey(objectKey)

        return if (minioStorageProperties.enabled) {
            readFromMinio(normalizedObjectKey)
        } else {
            readFromLocal(normalizedObjectKey)
        }
    }

    private fun readFromMinio(objectKey: String): SiteContentAssetPayload {
        val client = minioClientProvider.getIfAvailable()
            ?: throw IllegalStateException("MinIO가 활성화되어 있으나 MinioClient 빈을 사용할 수 없습니다.")

        val bytes = client.getObject(
            GetObjectArgs.builder()
                .bucket(minioStorageProperties.bucket)
                .`object`(objectKey)
                .build(),
        ).use { it.readBytes() }

        return SiteContentAssetPayload(
            bytes = bytes,
            contentType = resolveContentType(objectKey),
        )
    }

    private fun readFromLocal(objectKey: String): SiteContentAssetPayload {
        val path = resolveLocalPath(objectKey)
        if (!Files.exists(path)) {
            throw EntityNotFoundException("에셋을 찾을 수 없습니다. objectKey=$objectKey")
        }

        return SiteContentAssetPayload(
            bytes = Files.readAllBytes(path),
            contentType = Files.probeContentType(path) ?: resolveContentType(objectKey),
        )
    }

    private fun resolveLocalPath(objectKey: String): Path {
        val root = localBasePath.trim().takeIf { it.isNotEmpty() }
            ?: Paths.get(System.getProperty("user.home"), "Desktop", "image").toString()

        return Paths.get(root)
            .resolve(objectKey.replace("/", "\\"))
            .toAbsolutePath()
            .normalize()
    }

    private fun normalizeObjectKey(objectKey: String): String {
        val normalized = objectKey.trim().replace("\\", "/").removePrefix("/")
        require(normalized.startsWith("webzine/")) { "webzine/ 접두사가 있는 에셋 키만 허용됩니다." }
        return normalized
    }

    private fun resolveContentType(objectKey: String): String {
        val extension = objectKey.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            else -> "application/octet-stream"
        }
    }
}
