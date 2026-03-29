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
        require(uniqueFilename.isNotBlank()) { "저장 파일명은 비어 있을 수 없습니다." }
        require(content.isNotEmpty()) { "이미지 내용은 비어 있을 수 없습니다." }

        return StoredImage(
            url = "https://mock-cloud.example/images/$uniqueFilename",
            fileSize = content.size.toLong()
        )
    }
}
