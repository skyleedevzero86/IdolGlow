package com.sleekydz86.idolglow.global.infrastructure.exception

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.multipart.MaxUploadSizeExceededException

@DisplayName("업로드 용량 초과 응답")
class GlobalExceptionAdviceUploadSizeTest {
    @Test
    @DisplayName("업로드 용량을 넘기면 413과 안내 문구를 반환한다")
    fun returnsPayloadTooLargeWhenUploadExceedsLimit() {
        // given
        val advice = MaxUploadSizeExceptionAdvice()
        val request = MockHttpServletRequest("POST", "/admin/mim/uploads/images")

        // when
        val response =
            advice.handleMaxUploadSizeExceeded(
                request,
                MaxUploadSizeExceededException(150L * 1024 * 1024),
            )

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE)
        assertThat(response.body?.message).isEqualTo("이미지 업로드는 최대 150MB까지 가능합니다.")
    }
}
