package com.sleekydz86.idolglow.like.adapter.web

import com.jayway.jsonpath.JsonPath
import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import com.sleekydz86.idolglow.glowfeed.GlowFeedFixture
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.user.user.domain.User
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDate

@DisplayName("콘텐츠 공용 좋아요 API")
class ContentLikePostgresTest : PostgresIntegrationTest() {
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

    @DisplayName("비회원은 좋아요와 취소를 할 수 없고 401 을 받는다")
    @Test
    fun guestCannotLike() {
        // given
        val review = fixture.review(fixture.member(), hidden = false)
        val path = "/api/likes/PRODUCT_REVIEW/${review.id}"

        // when
        val liked = mockMvc.perform(post(path))
        val unliked = mockMvc.perform(delete(path))

        // then
        liked.andExpect(status().isUnauthorized)
        unliked.andExpect(status().isUnauthorized)
    }

    @DisplayName("회원은 좋아요를 누르고 취소하며 수는 회원마다 한 번만 센다")
    @Test
    fun memberLikesAndUnlikes() {
        // given
        val first = fixture.member()
        val second = fixture.member()
        val review = fixture.review(first, hidden = false)
        val path = "/api/likes/PRODUCT_REVIEW/${review.id}"

        // when
        val firstLike = perform(post(path), first)
        val secondLike = perform(post(path), second)
        val repeated = perform(post(path), first)
        val unliked = perform(delete(path), first)
        val memberFeed = perform(get("/api/glow/feed").param("category", "recent").param("size", "50"), second)
        val guestFeed = mockMvc.perform(get("/api/glow/feed").param("category", "recent").param("size", "50"))

        // then
        firstLike.andExpect(status().isOk).andExpect(jsonPath("$.liked").value(true)).andExpect(likeCount(1))
        secondLike.andExpect(status().isOk).andExpect(likeCount(2))
        repeated.andExpect(status().isOk).andExpect(jsonPath("$.liked").value(true)).andExpect(likeCount(2))
        unliked.andExpect(status().isOk).andExpect(jsonPath("$.liked").value(false)).andExpect(likeCount(1))
        val card = "$.items[?(@.id == 'PRODUCT_REVIEW-${review.id}')]"
        val memberBody = bodyOf(memberFeed)
        assertEquals(listOf(true), JsonPath.read<List<Boolean>>(memberBody, "$card.liked"))
        assertEquals(listOf(1), JsonPath.read<List<Int>>(memberBody, "$card.likeCount"))
        val guestBody = bodyOf(guestFeed)
        assertEquals(listOf(false), JsonPath.read<List<Boolean>>(guestBody, "$card.liked"))
    }

    @DisplayName("트렌드 기사는 호슬러그/기사슬러그 키로, 지역사회 글은 기존 게시글 좋아요로 처리한다")
    @Test
    fun articleAndCommunityTargets() {
        // given
        val member = fixture.member()
        val article = fixture.article(LocalDate.now(IdolGlowDateTimes.ZONE).minusDays(1), "${fixture.token} 기사")
        val post = fixture.post(fixture.member(), blinded = false)

        // when
        val articleLike = perform(post("/api/likes/ARTICLE/${article.key}"), member)
        val postLike = perform(post("/api/likes/glow-community-post/${post.id}"), member)
        val postDetail = perform(get("/api/glow-community/posts/${post.id}"), member)
        val postUnlike = perform(delete("/api/likes/GLOW_COMMUNITY_POST/${post.id}"), member)
        val missing = perform(post("/api/likes/PRODUCT_REVIEW/$MISSING_ID"), member)
        val invalidType = perform(post("/api/likes/NOTICE/1"), member)

        // then
        articleLike.andExpect(status().isOk).andExpect(jsonPath("$.liked").value(true)).andExpect(likeCount(1))
        postLike.andExpect(status().isOk).andExpect(jsonPath("$.liked").value(true)).andExpect(likeCount(1))
        postDetail.andExpect(jsonPath("$.liked").value(true)).andExpect(likeCount(1))
        postUnlike.andExpect(status().isOk).andExpect(jsonPath("$.liked").value(false)).andExpect(likeCount(0))
        missing.andExpect(status().isNotFound)
        invalidType.andExpect(status().isBadRequest)
    }

    private fun perform(
        request: MockHttpServletRequestBuilder,
        member: User,
    ) = mockMvc.perform(request.with(user(member.id.toString()).roles("USER")))

    private fun bodyOf(result: ResultActions): String =
        result
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

    private fun likeCount(expected: Int) = jsonPath("$.likeCount").value(expected)

    companion object {
        private const val MISSING_ID = 987_654_321L
    }
}
