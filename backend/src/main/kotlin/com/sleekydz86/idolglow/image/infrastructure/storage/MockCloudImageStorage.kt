package com.sleekydz86.idolglow.image.infrastructure.storage

import com.sleekydz86.idolglow.image.domain.domainservice.ImageStorage
import com.sleekydz86.idolglow.image.domain.domainservice.StoredImage
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Primary
@Profile("prod")
@Component
class MockCloudImageStorage : ImageStorage {



    override fun store(uniqueFilename: String, content: ByteArray): StoredImage {
        require(uniqueFilename.isNotBlank()) { "uniqueFilename must not be blank." }
        require(content.isNotEmpty()) { "content must not be empty." }

        return StoredImage(
            url = "https://mock-cloud.example/images/$uniqueFilename",
            fileSize = content.size.toLong()
        )
    }
}
