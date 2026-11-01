package com.sleekydz86.idolglow.global.infrastructure.exception

import org.apache.catalina.connector.ClientAbortException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.context.request.async.AsyncRequestNotUsableException

@DisplayName("응답 중단 예외")
class GlobalExceptionAdviceClientAbortTest {
    private val advice = GlobalExceptionAdvice()

    @Test
    fun `클라이언트가 이미지 수신을 끊으면 오류 본문을 쓰지 않는다`() {
        // given
        val request = MockHttpServletRequest("GET", "/site-content/assets")
        val response = MockHttpServletResponse()
        response.contentType = "image/jpeg"
        val exception =
            AsyncRequestNotUsableException(
                "ServletOutputStream failed to write",
                ClientAbortException("현재 연결은 사용자의 호스트 시스템의 소프트웨어의 의해 중단되었습니다"),
            )

        // when
        val result = advice.handleUnexpectedException(request, response, exception)

        // then
        assertThat(result).isNull()
    }

    @Test
    fun `일반 예외는 서버 오류 응답을 남긴다`() {
        // given
        val request = MockHttpServletRequest("GET", "/site-content/home")
        val response = MockHttpServletResponse()

        // when
        val result = advice.handleUnexpectedException(request, response, IllegalStateException("실패"))

        // then
        assertThat(result?.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(result?.body?.errorCode).isEqualTo("INTERNAL_SERVER_ERROR")
    }
}
