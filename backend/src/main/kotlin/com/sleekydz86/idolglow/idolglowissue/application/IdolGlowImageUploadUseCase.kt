package com.sleekydz86.idolglow.idolglowissue.application

import com.sleekydz86.idolglow.idolglowissue.application.dto.AdminIssueImageUploadResponse
import org.springframework.web.multipart.MultipartFile

interface IdolGlowImageUploadUseCase {
    fun upload(
        file: MultipartFile,
        folder: String?,
    ): AdminIssueImageUploadResponse
}
