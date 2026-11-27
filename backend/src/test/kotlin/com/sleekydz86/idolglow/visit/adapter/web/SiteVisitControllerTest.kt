package com.sleekydz86.idolglow.visit.adapter.web

import com.sleekydz86.idolglow.global.adapter.resolver.AuthenticatedPrincipalResolver
import com.sleekydz86.idolglow.visit.application.SiteVisitService
import com.sleekydz86.idolglow.visit.domain.VisitorId
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@DisplayName("방문 기록 컨트롤러")
class SiteVisitControllerTest {
    @DisplayName("저장 중 예외가 나도 204와 쿠키를 돌려준다")
    @Test
    fun respondsNoContentWhenRecordingFails() {
        // given
        val service = mockk<SiteVisitService>()
        every { service.resolveVisitorId(null) } returns "01234567-89ab-cdef-0123-456789abcdef"
        every { service.record(any(), any(), any()) } throws IllegalStateException("저장소 연결 실패")
        val resolver = mockk<AuthenticatedPrincipalResolver>()
        every { resolver.resolveOrNull() } returns null
        val mockMvc = MockMvcBuilders.standaloneSetup(SiteVisitController(service, resolver, false)).build()

        // when
        val result = mockMvc.perform(post("/visits"))

        // then
        result
            .andExpect(status().isNoContent)
            .andExpect(cookie().value(VisitorId.COOKIE_NAME, "01234567-89ab-cdef-0123-456789abcdef"))
    }
}
