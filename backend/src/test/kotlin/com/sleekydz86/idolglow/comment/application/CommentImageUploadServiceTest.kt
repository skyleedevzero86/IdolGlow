package com.sleekydz86.idolglow.comment.application

import com.sleekydz86.idolglow.comment.application.dto.CommentImageFile
import com.sleekydz86.idolglow.comment.application.port.out.CommentImageStoragePort
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("댓글 이미지 업로드")
class CommentImageUploadServiceTest {
    private val storage = mockk<CommentImageStoragePort>()
    private val service = CommentImageUploadService(storage)

    @DisplayName("이미지가 아닌 파일은 거절하고 저장소를 부르지 않는다")
    @Test
    fun rejectsNonImages() {
        // given
        val script = "<svg onload=alert(1)>".toByteArray()
        val text = "hello".toByteArray()

        // when
        val svgError = rejected { service.upload(script, "a.svg", "image/svg+xml") }
        val textError = rejected { service.upload(text, "a.png", "image/png") }
        val emptyError = rejected { service.upload(ByteArray(0), "a.png", "image/png") }

        // then
        assertEquals("PNG, JPG, GIF, WEBP 이미지만 올릴 수 있습니다.", svgError.message)
        assertEquals("PNG, JPG, GIF, WEBP 이미지만 올릴 수 있습니다.", textError.message)
        assertEquals("업로드할 이미지 파일을 선택해 주세요.", emptyError.message)
        verify(exactly = 0) { storage.store(any()) }
    }

    @DisplayName("PNG 는 저장하고 URL 을 돌려준다")
    @Test
    fun storesPng() {
        // given
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00)
        val captured = slot<CommentImageFile>()
        every { storage.store(capture(captured)) } returns "/api/mbrd/editor/images/abc"

        // when
        val result = service.upload(png, "photo.png", "image/png")

        // then
        assertEquals("/api/mbrd/editor/images/abc", result.url)
        assertEquals(".png", captured.captured.extension)
        assertEquals("image/png", captured.captured.contentType)
    }

    @DisplayName("10MB 를 넘거나 선언한 형식과 내용이 다르면 거절한다")
    @Test
    fun rejectsOversizeOrMismatchedType() {
        // given
        val jpegHead = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0x00)
        val oversized = ByteArray(10 * 1024 * 1024 + 1).also { jpegHead.copyInto(it) }

        // when
        val sizeError = rejected { service.upload(oversized, "big.jpg", "image/jpeg") }
        val mismatchError = rejected { service.upload(jpegHead, "a.png", "image/png") }

        // then
        assertEquals("댓글 이미지는 최대 10MB까지 올릴 수 있습니다.", sizeError.message)
        assertEquals("이미지 형식과 파일 형식이 일치하지 않습니다.", mismatchError.message)
    }

    private fun rejected(action: () -> Unit): IllegalArgumentException =
        assertThrows(IllegalArgumentException::class.java) { action() }
}
