package com.sleekydz86.idolglow.webzine.application

import com.sleekydz86.idolglow.global.config.AppPublicUrlProperties
import com.sleekydz86.idolglow.global.config.MinioStorageProperties
import com.sleekydz86.idolglow.productpackage.admin.application.AdminAuditService
import com.sleekydz86.idolglow.webzine.application.dto.AdminIssueImageUploadResponse
import io.minio.MinioClient
import io.minio.PutObjectArgs
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

@Service
class WebzineImageUploadService(
    private val minioProps: MinioStorageProperties,
    private val publicUrlProps: AppPublicUrlProperties,
    private val minioClientProvider: ObjectProvider<MinioClient>,
    private val adminAuditService: AdminAuditService,
    @Value("\${app.storage.local.base-path:}") private val localBasePath: String,
) {

    fun upload(file: MultipartFile, folder: String?): AdminIssueImageUploadResponse {
        require(!file.isEmpty) { "업로드할 이미지 파일을 선택해 주세요." }

        val bytes = file.bytes
        require(bytes.isNotEmpty()) { "업로드할 이미지 파일을 선택해 주세요." }
        require(bytes.size <= MAX_BYTES) { "이미지 파일은 15MB 이하만 업로드할 수 있습니다." }

        val contentType = file.contentType?.lowercase()?.trim().orEmpty()
        val ext = extensionFor(contentType, file.originalFilename)
            ?: throw IllegalArgumentException("JPG, PNG, WebP, GIF 이미지만 업로드할 수 있습니다.")

        val safeFolder = sanitizeFolder(folder)
        val filename = "${UUID.randomUUID()}$ext"
        val objectKey = "webzine/$safeFolder/$filename"
        val resolvedContentType = resolvedContentType(contentType, ext)

        val url = if (minioProps.enabled) {
            val client = minioClientProvider.getIfAvailable()
                ?: throw IllegalStateException("app.storage.minio.enabled=true 인데 MinioClient 빈이 없습니다.")

            ByteArrayInputStream(bytes).use { stream ->
                client.putObject(
                    PutObjectArgs.builder()
                        .bucket(minioProps.bucket)
                        .`object`(objectKey)
                        .stream(stream, bytes.size.toLong(), -1)
                        .contentType(resolvedContentType)
                        .build()
                )
            }

            "${minioProps.publicBaseUrl.trimEnd('/')}/$objectKey"
        } else {
            val root = localBasePath.trim().takeIf { it.isNotEmpty() }
                ?: Paths.get(System.getProperty("user.home"), "Desktop", "image").toString()
            val dir = Paths.get(root, "webzine", safeFolder)
            Files.createDirectories(dir)
            Files.write(dir.resolve(filename), bytes)
            "${publicUrlProps.publicBaseUrl.trimEnd('/')}/uploads/webzine/$safeFolder/$filename"
        }

        adminAuditService.log(
            actionCode = "WEBZINE_IMAGE_UPLOAD",
            targetType = "WEBZINE_ASSET",
            targetId = null,
            detail = "folder=$safeFolder, objectKey=$objectKey",
        )

        return AdminIssueImageUploadResponse(
            url = url,
            objectKey = objectKey,
            contentType = resolvedContentType,
            size = bytes.size.toLong(),
        )
    }

    private fun sanitizeFolder(folder: String?): String {
        val raw = folder?.trim().orEmpty()
        if (raw.isBlank()) {
            return "common"
        }

        val normalized = raw
            .replace("\\", "/")
            .split("/")
            .map { part ->
                part.lowercase()
                    .replace(Regex("[^a-z0-9_-]"), "-")
                    .replace(Regex("-+"), "-")
                    .trim('-')
            }
            .filter { it.isNotBlank() }
            .joinToString("/")

        return normalized.ifBlank { "common" }
    }

    private fun extensionFor(contentType: String, originalFilename: String?): String? {
        val fromContentType = when (contentType) {
            "image/jpeg", "image/jpg" -> ".jpg"
            "image/png" -> ".png"
            "image/webp" -> ".webp"
            "image/gif" -> ".gif"
            else -> null
        }
        if (fromContentType != null) {
            return fromContentType
        }

        return when (originalFilename?.substringAfterLast('.', "")?.lowercase().orEmpty()) {
            "jpg", "jpeg" -> ".jpg"
            "png" -> ".png"
            "webp" -> ".webp"
            "gif" -> ".gif"
            else -> null
        }
    }

    private fun resolvedContentType(contentType: String, ext: String): String =
        contentType.ifBlank {
            when (ext) {
                ".jpg" -> "image/jpeg"
                ".png" -> "image/png"
                ".webp" -> "image/webp"
                ".gif" -> "image/gif"
                else -> "application/octet-stream"
            }
        }

    companion object {
        private const val MAX_BYTES = 15 * 1024 * 1024
    }
}
