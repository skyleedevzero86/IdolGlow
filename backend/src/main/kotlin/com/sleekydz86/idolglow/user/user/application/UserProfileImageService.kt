package com.sleekydz86.idolglow.user.user.application

import com.sleekydz86.idolglow.global.config.AppPublicUrlProperties
import com.sleekydz86.idolglow.global.config.MinioStorageProperties
import com.sleekydz86.idolglow.global.exceptions.CustomException
import com.sleekydz86.idolglow.global.exceptions.UserExceptionType
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
class UserProfileImageService(
    private val minioProps: MinioStorageProperties,
    private val publicUrlProps: AppPublicUrlProperties,
    private val minioClientProvider: ObjectProvider<MinioClient>,
    @Value("\${app.storage.local.base-path:}") private val localBasePath: String,
) {

    fun uploadAndGetPublicUrl(userId: Long, file: MultipartFile): String {
        if (file.isEmpty) {
            throw CustomException(UserExceptionType.PROFILE_IMAGE_INVALID_TYPE)
        }
        val bytes = file.bytes
        if (bytes.isEmpty()) {
            throw CustomException(UserExceptionType.PROFILE_IMAGE_INVALID_TYPE)
        }
        if (bytes.size > MAX_BYTES) {
            throw CustomException(UserExceptionType.PROFILE_IMAGE_TOO_LARGE)
        }
        val contentType = file.contentType?.lowercase()?.trim().orEmpty()
        val ext = extensionForContentType(contentType, file.originalFilename)
            ?: throw CustomException(UserExceptionType.PROFILE_IMAGE_INVALID_TYPE)

        val filename = "${UUID.randomUUID()}$ext"
        return if (minioProps.enabled) {
            val client = minioClientProvider.getIfAvailable()
                ?: throw IllegalStateException("app.storage.minio.enabled=true 인데 MinioClient 빈이 없습니다.")
            val key = "profiles/$userId/$filename"
            val objectContentType = contentType.ifBlank {
                when (ext) {
                    ".jpg" -> "image/jpeg"
                    ".png" -> "image/png"
                    ".webp" -> "image/webp"
                    else -> "application/octet-stream"
                }
            }
            ByteArrayInputStream(bytes).use { stream ->
                client.putObject(
                    PutObjectArgs.builder()
                        .bucket(minioProps.bucket)
                        .`object`(key)
                        .stream(stream, bytes.size.toLong(), -1)
                        .contentType(objectContentType)
                        .build()
                )
            }
            "${minioProps.publicBaseUrl.trimEnd('/')}/$key"
        } else {
            val root = localBasePath.trim().takeIf { it.isNotEmpty() }
                ?: Paths.get(System.getProperty("user.home"), "Desktop", "image").toString()
            val dir = Paths.get(root, "profile-avatars", userId.toString())
            Files.createDirectories(dir)
            val path = dir.resolve(filename)
            Files.write(path, bytes)
            "${publicUrlProps.publicBaseUrl.trimEnd('/')}/uploads/profile-avatars/$userId/$filename"
        }
    }

    private fun extensionForContentType(contentType: String, originalFilename: String?): String? {
        val fromCt = when (contentType) {
            "image/jpeg", "image/jpg" -> ".jpg"
            "image/png" -> ".png"
            "image/webp" -> ".webp"
            else -> null
        }
        if (fromCt != null) return fromCt
        val tail = originalFilename?.substringAfterLast('.', "")?.lowercase().orEmpty()
        return when (tail) {
            "jpg", "jpeg" -> ".jpg"
            "png" -> ".png"
            "webp" -> ".webp"
            else -> null
        }
    }

    companion object {
        private const val MAX_BYTES = 5 * 1024 * 1024
    }
}
