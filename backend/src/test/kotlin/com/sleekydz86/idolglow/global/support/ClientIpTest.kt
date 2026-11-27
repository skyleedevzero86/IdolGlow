package com.sleekydz86.idolglow.global.support

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest

@DisplayName("로그인·가입·복구 감사용 접속 IP")
class ClientIpTest {
    @DisplayName("요청이 보낸 X-Forwarded-For 는 읽지 않고 접속 주소만 쓴다")
    @Test
    fun ignoresForwardedHeader() {
        // given
        val request = MockHttpServletRequest()
        request.remoteAddr = "192.0.2.10"
        request.addHeader("X-Forwarded-For", "198.51.100.88, 10.0.0.1")

        // when
        val ip = ClientIp.of(request)

        // then
        assertEquals("192.0.2.10", ip)
    }

    @DisplayName("접속 주소가 비어 있으면 unknown 으로 남긴다")
    @Test
    fun fallsBackToUnknown() {
        // given
        val request = MockHttpServletRequest()
        request.remoteAddr = ""

        // when
        val ip = ClientIp.of(request)

        // then
        assertEquals(ClientIp.UNKNOWN, ip)
    }
}
