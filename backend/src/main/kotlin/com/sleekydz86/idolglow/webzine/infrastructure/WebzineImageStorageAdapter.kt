package com.sleekydz86.idolglow.webzine.infrastructure

import com.sleekydz86.idolglow.global.config.AppPublicUrlProperties
import com.sleekydz86.idolglow.global.config.MinioStorageProperties
import com.sleekydz86.idolglow.webzine.application.StoreWebzineImageCommand
import com.sleekydz86.idolglow.webzine.application.StoredWebzineImage
import com.sleekydz86.idolglow.webzine.application.WebzineImageStoragePort
import io.minio.MinioClient
import io.minio.PutObjectArgs
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

@Component
class WebzineImageStorageAdapter(
    private val minioProps: MinioStorageProperties,
    private val publicUrlProps: AppPublicUrlProperties,
    private val minioClientProvider: ObjectProvider<MinioClient>,
    @Value("\${app.storage.local.base-path:}") private val localBasePath: String,
) : WebzineImageStoragePort {

    override fun store(command: StoreWebzineImageCommand): StoredWebzineImage {
        val filename = "${UUID.randomUUID()}${command.extension}"
        val objectKey = "webzine/${command.folder}/$filename"

        val url = if (minioProps.enabled) {
            val client = minioClientProvider.getIfAvailable()
                ?: throw IllegalStateException("app.storage.minio.enabled=true 인데 MinioClient 빈이 없습니다.")

            ByteArrayInputStream(command.bytes).use { stream ->
                client.putObject(
                    PutObjectArgs.builder()
                        .bucket(minioProps.bucket)
                        .`object`(objectKey)
                        .stream(stream, command.bytes.size.toLong(), -1)
                        .contentType(command.contentType)
                        .build()
                )
            }

            "${minioProps.publicBaseUrl.trimEnd('/')}/$objectKey"
        } else {
            val root = localBasePath.trim().takeIf { it.isNotEmpty() }
                ?: Paths.get(System.getProperty("user.home"), "Desktop", "image").toString()
            val dir = Paths.get(root, "webzine", command.folder)
            Files.createDirectories(dir)
            Files.write(dir.resolve(filename), command.bytes)
            "${publicUrlProps.publicBaseUrl.trimEnd('/')}/uploads/webzine/${command.folder}/$filename"
        }

        return StoredWebzineImage(
            url = url,
            objectKey = objectKey,
            contentType = command.contentType,
            size = command.bytes.size.toLong(),
        )
    }
}
