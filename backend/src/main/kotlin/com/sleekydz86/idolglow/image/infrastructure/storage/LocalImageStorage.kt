package com.sleekydz86.idolglow.image.infrastructure.storage

import com.sleekydz86.idolglow.image.domain.domainservice.ImageStorage
import com.sleekydz86.idolglow.image.domain.domainservice.StoredImage
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

@Primary
@Profile("local", "test", "dev")
@Component
class LocalImageStorage(
    @Value("\${app.storage.local.base-path:}") basePath: String? = null
) : ImageStorage {

    private val rootPath: Path = basePath
        ?.takeIf { it.isNotBlank() }
        ?.let { Paths.get(it) }
        ?: Paths.get(System.getProperty("user.home"), "Desktop", "image")

    override fun store(uniqueFilename: String, content: ByteArray): StoredImage {
        require(uniqueFilename.isNotBlank()) { "uniqueFilename must not be blank." }
        require(content.isNotEmpty()) { "content must not be empty." }

        Files.createDirectories(rootPath)
        val targetPath = rootPath.resolve(uniqueFilename)
        Files.write(
            targetPath,
            content,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        )
        return StoredImage(
            url = targetPath.toUri().toString(),
            fileSize = content.size.toLong()
        )
    }
}
