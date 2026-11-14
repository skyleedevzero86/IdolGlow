package com.sleekydz86.idolglow.webzine.adapter.web

import com.jayway.jsonpath.JsonPath
import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import com.sleekydz86.idolglow.glowfeed.GlowFeedFixture
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDate

@DisplayName("웹진 기사 카드 정렬 API")
class WebzineArticleRankingPostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    private lateinit var mockMvc: MockMvc

    private lateinit var fixture: GlowFeedFixture

    @BeforeEach
    fun setUp() {
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .apply<DefaultMockMvcBuilder>(springSecurity())
                .build()
        fixture = GlowFeedFixture(applicationContext)
    }

    @AfterEach
    fun tearDown() {
        fixture.cleanup()
    }

    @DisplayName("최신순·인기순·공감순으로 다시 정렬하고 예약 호 기사는 빼며 비회원도 볼 수 있다")
    @Test
    fun sortsPublishedArticles() {
        // given
        val today = LocalDate.now(IdolGlowDateTimes.ZONE)
        val oldest = fixture.article(today.minusDays(OLDEST_DAYS), "${fixture.token} 오래된 기사")
        val middle = fixture.article(today.minusDays(MIDDLE_DAYS), "${fixture.token} 중간 기사")
        val newest = fixture.article(today, "${fixture.token} 최신 기사")
        val scheduled = fixture.article(today.plusMonths(1), "${fixture.token} 예약 기사")
        fixture.react("ARTICLE", null, oldest.key, count = 40)
        fixture.react("ARTICLE", null, middle.key, count = 10)
        fixture.react(null, "ARTICLE", middle.key, count = 45)
        val member = fixture.member()
        mockMvc.perform(post("/api/likes/ARTICLE/${newest.key}").with(user(member.id.toString()).roles("USER")))
        val mine = listOf(oldest, middle, newest, scheduled).map { it.key }

        // when
        val latest = mockMvc.perform(articles("latest"))
        val popular = mockMvc.perform(articles("popular"))
        val likes = mockMvc.perform(articles("likes"))
        val asMember = mockMvc.perform(articles("latest").with(user(member.id.toString()).roles("USER")))

        // then
        assertEquals(listOf(newest.key, middle.key, oldest.key), keys(latest, mine))
        assertEquals(listOf(middle.key, oldest.key, newest.key), keys(popular, mine))
        assertEquals(listOf(oldest.key, middle.key, newest.key), keys(likes, mine))
        val popularBody = bodyOf(popular)
        val middleCard = "$.items[?(@.likeTargetKey == '${middle.key}')]"
        assertEquals(listOf(10), JsonPath.read<List<Int>>(popularBody, "$middleCard.likeCount"))
        assertEquals(listOf(45), JsonPath.read<List<Int>>(popularBody, "$middleCard.viewerCount"))
        assertEquals(listOf(middle.volume), JsonPath.read<List<Int>>(popularBody, "$middleCard.volume"))
        val newestCard = "$.items[?(@.likeTargetKey == '${newest.key}')]"
        val memberBody = bodyOf(asMember)
        assertEquals(listOf(true), JsonPath.read<List<Boolean>>(memberBody, "$newestCard.liked"))
        assertEquals(listOf(false), JsonPath.read<List<Boolean>>(popularBody, "$newestCard.liked"))
    }

    private fun bodyOf(result: ResultActions): String =
        result
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

    private fun articles(sort: String) = get("/api/webzine/articles").param("sort", sort).param("size", "50")

    private fun keys(
        result: ResultActions,
        mine: List<String>,
    ): List<String> {
        val body = bodyOf(result)
        return JsonPath.read<List<String>>(body, "$.items[*].likeTargetKey").filter { it in mine }
    }

    companion object {
        private const val OLDEST_DAYS = 20L
        private const val MIDDLE_DAYS = 10L
    }
}
