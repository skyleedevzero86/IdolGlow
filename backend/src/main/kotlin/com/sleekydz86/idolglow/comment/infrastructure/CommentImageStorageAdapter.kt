package com.sleekydz86.idolglow.comment.infrastructure

import com.sleekydz86.idolglow.comment.application.dto.CommentImageFile
import com.sleekydz86.idolglow.comment.application.port.out.CommentImageStoragePort
import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.comment.ContentCommentExceptionType
import com.sleekydz86.idolglow.idolglowissue.application.IdolGlowImageStoragePort
import com.sleekydz86.idolglow.idolglowissue.application.StoreIdolGlowImageCommand
import com.sleekydz86.idolglow.mbrd.infrastructure.storage.MbrdMinioEditorImageStorageService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

@Component
class CommentImageStorageAdapter(
    private val editorImageStorage: ObjectProvider<MbrdMinioEditorImageStorageService>,
    private val idolglowImageStoragePort: ObjectProvider<IdolGlowImageStoragePort>,
) : CommentImageStoragePort {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun store(image: CommentImageFile): String {
        val editor = editorImageStorage.getIfAvailable()
        if (editor != null) {
            val uploaded = runCatching { editor.upload(ByteArrayMultipartFile(image)).imageUrl }
            uploaded.exceptionOrNull()?.let { log.warn("댓글 이미지 에디터 저장 실패, 웹진 저장소로 재시도 error={}", it.message) }
            uploaded.getOrNull()?.let { return it }
        }
        val fallback =
            idolglowImageStoragePort.getIfAvailable()
                ?: throw CustomException(ContentCommentExceptionType.IMAGE_STORAGE_UNAVAILABLE)
        return fallback
            .store(
                StoreIdolGlowImageCommand(
                    bytes = image.bytes,
                    originalFilename = image.originalFilename,
                    contentType = image.contentType,
                    extension = image.extension,
                    folder = FALLBACK_FOLDER,
                ),
            ).url
    }

    private class ByteArrayMultipartFile(
        private val image: CommentImageFile,
    ) : MultipartFile {
        override fun getName(): String = FIELD_NAME

        override fun getOriginalFilename(): String = image.originalFilename ?: "comment-image${image.extension}"

        override fun getContentType(): String = image.contentType

        override fun isEmpty(): Boolean = image.bytes.isEmpty()

        override fun getSize(): Long = image.bytes.size.toLong()

        override fun getBytes(): ByteArray = image.bytes

        override fun getInputStream(): InputStream = ByteArrayInputStream(image.bytes)

        override fun transferTo(dest: File) {
            dest.writeBytes(image.bytes)
        }
    }

    companion object {
        private const val FIELD_NAME = "file"
        private const val FALLBACK_FOLDER = "comments"
    }
}
