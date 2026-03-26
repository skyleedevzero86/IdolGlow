package com.sleekydz86.idolglow.productpackage.option.application.dto

import org.springframework.web.multipart.MultipartFile

data class OptionImageFile(
    val originalFilename: String,
    val content: ByteArray,
    val sortOrder: Int = 0,
) {

    companion object {
        fun from(images: List<MultipartFile>?): List<OptionImageFile> =
            images.orEmpty()
                .filterNot { it.isEmpty }
                .mapIndexed { index, file ->
                    OptionImageFile(
                        originalFilename = file.originalFilename ?: "option-image-$index",
                        content = file.bytes,
                        sortOrder = index
                    )
                }
    }
}
