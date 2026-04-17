package com.sleekydz86.idolglow.webzine.application

import com.sleekydz86.idolglow.webzine.application.dto.AdminIssueImageUploadResponse
import org.springframework.web.multipart.MultipartFile

interface WebzineImageUploadUseCase {
    fun upload(file: MultipartFile, folder: String?): AdminIssueImageUploadResponse
}
