package com.sleekydz86.idolglow.comment.application

import com.sleekydz86.idolglow.comment.application.dto.CommentImageFile
import com.sleekydz86.idolglow.comment.application.dto.CommentImageUploadResult
import com.sleekydz86.idolglow.comment.application.port.`in`.CommentImageUploadUseCase
import com.sleekydz86.idolglow.comment.application.port.out.CommentImageStoragePort
import org.springframework.stereotype.Service
import java.util.Locale

@Service
class CommentImageUploadService(
    private val commentImageStoragePort: CommentImageStoragePort,
) : CommentImageUploadUseCase {
    override fun upload(
        bytes: ByteArray,
        originalFilename: String?,
        contentType: String?,
    ): CommentImageUploadResult {
        require(bytes.isNotEmpty()) { "업로드할 이미지 파일을 선택해 주세요." }
        require(bytes.size <= MAX_BYTES) { "댓글 이미지는 최대 10MB까지 올릴 수 있습니다." }
        val format =
            FORMATS.firstOrNull { it.matches(bytes) }
                ?: throw IllegalArgumentException("PNG, JPG, GIF, WEBP 이미지만 올릴 수 있습니다.")
        val declared = contentType?.trim()?.lowercase(Locale.ROOT).orEmpty()
        require(declared.isEmpty() || declared in format.contentTypes) { "이미지 형식과 파일 형식이 일치하지 않습니다." }
        val url =
            commentImageStoragePort.store(
                CommentImageFile(
                    bytes = bytes,
                    originalFilename = originalFilename,
                    contentType = format.contentTypes.first(),
                    extension = format.extension,
                ),
            )
        return CommentImageUploadResult(url = url)
    }

    private class ImageFormat(
        val extension: String,
        val contentTypes: List<String>,
        private val signatures: Map<Int, IntArray>,
    ) {
        fun matches(bytes: ByteArray): Boolean = signatures.all { (offset, expected) -> bytes.hasAt(offset, expected) }

        private fun ByteArray.hasAt(
            offset: Int,
            expected: IntArray,
        ): Boolean =
            size >= offset + expected.size &&
                expected.indices.all { (this[offset + it].toInt() and BYTE_MASK) == expected[it] }
    }

    companion object {
        private const val MAX_BYTES = 10 * 1024 * 1024
        private const val BYTE_MASK = 0xFF
        private const val WEBP_MARK_OFFSET = 8
        private val FORMATS =
            listOf(
                ImageFormat(".png", listOf("image/png"), mapOf(0 to intArrayOf(0x89, 0x50, 0x4E, 0x47))),
                ImageFormat(".jpg", listOf("image/jpeg", "image/jpg"), mapOf(0 to intArrayOf(0xFF, 0xD8, 0xFF))),
                ImageFormat(".gif", listOf("image/gif"), mapOf(0 to intArrayOf(0x47, 0x49, 0x46, 0x38))),
                ImageFormat(
                    ".webp",
                    listOf("image/webp"),
                    mapOf(
                        0 to intArrayOf(0x52, 0x49, 0x46, 0x46),
                        WEBP_MARK_OFFSET to intArrayOf(0x57, 0x45, 0x42, 0x50),
                    ),
                ),
            )
    }
}
