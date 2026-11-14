package com.sleekydz86.idolglow.glowcommunity.application

import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityKeywordsResult
import com.sleekydz86.idolglow.glowcommunity.application.port.out.GlowCommunityKeywordGeneratorPort
import com.sleekydz86.idolglow.glowcommunity.application.port.out.GlowCommunityPostPort
import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunityPost
import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunityTag
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("지역사회 추천 검색어")
class GlowCommunityKeywordServiceTest {
    private val postPort = mockk<GlowCommunityPostPort>()
    private val generator = mockk<GlowCommunityKeywordGeneratorPort>()
    private val service = GlowCommunityKeywordService(postPort, generator)

    @DisplayName("LLM 을 쓸 수 없으면 최근 글 제목에서 자주 나온 단어로 대신한다")
    @Test
    fun fallsBackToFrequentTitleWordsWhenLlmUnavailable() {
        // given
        stubPosts(
            post(1, "명동 맛집 추천해 주세요"),
            post(2, "명동 맛집 어디가 좋아요?"),
            post(3, "명동에서 한복 대여"),
            post(4, "성수 팝업 오픈런"),
        )
        every { generator.generate(any()) } returns null

        // when
        val result = service.refresh()

        // then
        assertEquals(GlowCommunityKeywordsResult.SOURCE_FALLBACK, result.source)
        assertEquals("명동", result.keywords.first())
        assertEquals("맛집", result.keywords[1])
        assertTrue(result.keywords.containsAll(listOf("한복", "대여", "성수", "팝업")))
        assertTrue(result.keywords.none { it == "추천해" || it == "주세요" })
    }

    @DisplayName("LLM 이 쓸 만한 검색어를 주지 않으면 대체 검색어를 쓴다")
    @Test
    fun fallsBackWhenLlmReturnsTooFew() {
        // given
        stubPosts(post(1, "부산 당일치기 KTX 일정"))
        every { generator.generate(any()) } returns listOf("부산")

        // when
        val result = service.refresh()

        // then
        assertEquals(GlowCommunityKeywordsResult.SOURCE_FALLBACK, result.source)
        assertTrue(result.keywords.contains("부산"))
    }

    @DisplayName("LLM 검색어가 충분하면 그대로 쓰고 요청 때는 다시 부르지 않는다")
    @Test
    fun usesLlmKeywordsAndCachesThem() {
        // given
        stubPosts(post(1, "제주 렌터카 없이 여행"))
        val keywords = listOf("제주 버스 여행", "명동 맛집", "한복 대여", "성수 팝업", "남산타워 야경", "광장시장 빈대떡")
        every { generator.generate(any()) } returns keywords
        service.refresh()

        // when
        val first = service.findKeywords()
        val second = service.findKeywords()

        // then
        assertEquals(GlowCommunityKeywordsResult.SOURCE_LLM, first.source)
        assertEquals(keywords, first.keywords)
        assertEquals(first, second)
        verify(exactly = 1) { generator.generate(any()) }
    }

    @DisplayName("글이 없으면 빈 목록을 주고 LLM 을 부르지 않는다")
    @Test
    fun emptyWhenNoPosts() {
        // given
        stubPosts()

        // when
        val refreshed = service.refresh()
        val requested = service.findKeywords()

        // then
        assertTrue(refreshed.keywords.isEmpty())
        assertTrue(requested.keywords.isEmpty())
        verify(exactly = 0) { generator.generate(any()) }
    }

    @DisplayName("요청 때 캐시가 없으면 LLM 없이 대체 검색어만 만든다")
    @Test
    fun requestPathNeverCallsLlm() {
        // given
        stubPosts(post(1, "강릉 바다 당일 여행"), post(2, "강릉 카페 추천"))

        // when
        val result = service.findKeywords()

        // then
        assertEquals(GlowCommunityKeywordsResult.SOURCE_FALLBACK, result.source)
        assertEquals("강릉", result.keywords.first())
        verify(exactly = 0) { generator.generate(any()) }
    }

    private fun stubPosts(vararg posts: GlowCommunityPost) {
        every { postPort.findRecentVisible(any()) } returns posts.toList()
        every { postPort.findPopularVisible(any()) } returns posts.toList()
    }

    private fun post(
        id: Long,
        title: String,
    ): GlowCommunityPost =
        GlowCommunityPost(
            id = id,
            userId = 1L,
            tag = GlowCommunityTag.TALK,
            title = title,
            contentMarkdown = "본문 **강조** 와 https://example.com 링크",
        )
}
