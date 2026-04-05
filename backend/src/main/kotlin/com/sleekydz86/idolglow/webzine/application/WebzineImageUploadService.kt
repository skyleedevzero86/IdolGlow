package com.sleekydz86.idolglow.webzine.application

import com.sleekydz86.idolglow.productpackage.admin.application.AdminAuditService
import com.sleekydz86.idolglow.webzine.application.dto.AdminIssueImageUploadResponse
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class WebzineImageUploadService(
    private val webzineImageStoragePort: WebzineImageStoragePort,
    private val adminAuditService: AdminAuditService,
) : WebzineImageUploadUseCase {

    override fun upload(file: MultipartFile, folder: String?): AdminIssueImageUploadResponse {
        require(!file.isEmpty) { "Please select an image file to upload." }

        val bytes = file.bytes
        require(bytes.isNotEmpty()) { "Please select an image file to upload." }
        require(bytes.size <= MAX_BYTES) { "Image uploads are limited to 15MB." }

        val contentType = file.contentType?.lowercase()?.trim().orEmpty()
        val ext = extensionFor(contentType, file.originalFilename)
            ?: throw IllegalArgumentException("Only JPG, PNG, WebP, and GIF files are allowed.")

        val safeFolder = sanitizeFolder(folder)
        val resolvedContentType = resolvedContentType(contentType, ext)
        val storedImage = webzineImageStoragePort.store(
            StoreWebzineImageCommand(
                bytes = bytes,
                originalFilename = file.originalFilename,
                contentType = resolvedContentType,
                extension = ext,
                folder = safeFolder,
            )
        )

        adminAuditService.log(
            actionCode = "WEBZINE_IMAGE_UPLOAD",
            targetType = "WEBZINE_ASSET",
            targetId = null,
            detail = "folder=$safeFolder, objectKey=${storedImage.objectKey}",
        )

        return AdminIssueImageUploadResponse(
            url = storedImage.url,
            objectKey = storedImage.objectKey,
            contentType = storedImage.contentType,
            size = storedImage.size,
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
