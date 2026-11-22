package com.sleekydz86.idolglow.glowcommunity.infrastructure

import com.sleekydz86.idolglow.global.llm.LlmGateway
import com.sleekydz86.idolglow.global.llm.LlmInputMasker
import com.sleekydz86.idolglow.global.llm.LlmRequest
import com.sleekydz86.idolglow.global.llm.LlmResult
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityKeywordSource
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper

@DisplayName("지역사회 추천 검색어 LLM 연결")
class GlowCommunityKeywordLlmAdapterTest {
    private val gateway = mockk<LlmGateway>()
    private val adapter = GlowCommunityKeywordLlmAdapter(gateway, LlmInputMasker(), ObjectMapper())
    private val sources = listOf(GlowCommunityKeywordSource("명동 맛집 문의 hello@example.com", "점심 추천"))

    @DisplayName("LLM 을 쓸 수 없으면 null 을 준다")
    @Test
    fun nullWhenGatewayUnavailable() {
        // given
        every { gateway.complete(any()) } returns null

        // when
        val result = adapter.generate(sources)

        // then
        assertNull(result)
    }

    @DisplayName("JSON 이 아니거나 쓸 만한 검색어가 5개보다 적으면 null 을 준다")
    @Test
    fun nullWhenJunk() {
        // given
        every { gateway.complete(any()) } returns
            LlmResult("""{"keywords":["명동","https://bad.example","a","@nick","test only"]}""", "local", 10)

        // when
        val result = adapter.generate(sources)

        // then
        assertNull(result)
    }

    @DisplayName("검증된 검색어만 최대 8개 돌려주고 이메일은 가려서 보낸다")
    @Test
    fun parsesValidKeywordsAndMasksEmail() {
        // given
        val request = slot<LlmRequest>()
        every { gateway.complete(capture(request)) } returns
            LlmResult(
                """```json
                {"keywords":["명동 맛집","#한복 대여","성수 팝업","남산 야경","광장시장","명동 맛집","부산 KTX","강릉 바다","제주 버스","경주 한옥"]}
                ```""",
                "local",
                10,
            )

        // when
        val result = adapter.generate(sources)

        // then
        assertEquals(listOf("명동 맛집", "한복 대여", "성수 팝업", "남산 야경", "광장시장", "부산 KTX", "강릉 바다", "제주 버스"), result)
        assertFalse(request.captured.userPrompt.contains("hello@example.com"))
        assertEquals("glow-community", request.captured.feature)
    }
}
