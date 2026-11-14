package com.sleekydz86.idolglow.glowfeed.adapter.web

import com.jayway.jsonpath.JsonPath
import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import com.sleekydz86.idolglow.glowfeed.GlowFeedFixture
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("GLOW 전체 보기 API")
class GlowFeedPostgresTest : PostgresIntegrationTest() {
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

    @DisplayName("비회원도 모든 카테고리를 최신순으로 섞어 보고 예약·숨김·삭제·블라인드 글은 빠진다")
    @Test
    fun feedMergesCategoriesNewestFirst() {
        // given
        val author = fixture.member()
        val article = fixture.article(today().minusDays(1), "${fixture.token} 기사")
        val scheduled = fixture.article(today().plusMonths(1), "${fixture.token} 예약")
        val event = fixture.event(today().plusDays(EVENT_DAYS_LEFT))
        val ended = fixture.event(today().minusDays(2))
        val review = fixture.review(author, hidden = false)
        val hiddenReview = fixture.review(author, hidden = true)
        val comment = fixture.comment(author, review.product.id.toString(), deleted = false)
        val deletedComment = fixture.comment(author, review.product.id.toString(), deleted = true)
        val post = fixture.post(author, blinded = false)
        val blinded = fixture.post(author, blinded = true)
        val visible =
            listOf(
                Triple("webzine_articles", article.id, "ARTICLE-${article.id}"),
                Triple("event_source_ref", event.id, "EVENT-${event.id}"),
                Triple("product_reviews", review.id, "PRODUCT_REVIEW-${review.id}"),
                Triple("content_comments", comment.id, "COMMENT-${comment.id}"),
                Triple("glow_community_posts", post.id, "COMMUNITY_POST-${post.id}"),
            )
        val hidden =
            listOf(
                Triple("webzine_articles", scheduled.id, "ARTICLE-${scheduled.id}"),
                Triple("event_source_ref", ended.id, "EVENT-${ended.id}"),
                Triple("product_reviews", hiddenReview.id, "PRODUCT_REVIEW-${hiddenReview.id}"),
                Triple("content_comments", deletedComment.id, "COMMENT-${deletedComment.id}"),
                Triple("glow_community_posts", blinded.id, "COMMUNITY_POST-${blinded.id}"),
            )
        (visible + hidden).forEachIndexed { index, (table, id, _) ->
            fixture.createdAt(table, id, MERGE_BASE.plusMinutes(index.toLong()))
        }

        // when
        val all = mockMvc.perform(get("/api/glow/feed").param("size", "50"))
        val recent = mockMvc.perform(get("/api/glow/feed").param("category", "recent").param("size", "50"))

        // then
        val allBody = bodyOf(all)
        val ids = JsonPath.read<List<String>>(allBody, "$.items[*].id")
        val mine = ids.filter { id -> (visible + hidden).any { it.third == id } }
        assertEquals(visible.map { it.third }.reversed(), mine)
        assertTrue(JsonPath.read<List<Boolean>>(allBody, "$.items[*].liked").none { it })
        val articleCard = "$.items[?(@.id == 'ARTICLE-${article.id}')]"
        assertEquals(listOf(article.volume), JsonPath.read<List<Int>>(allBody, "$articleCard.volume"))
        assertEquals(
            listOf("/glow?tab=trend&issue=${article.issueSlug}&article=${article.articleSlug}"),
            JsonPath.read<List<String>>(allBody, "$articleCard.path"),
        )
        val recentBody = bodyOf(recent)
        assertEquals(setOf("recent"), JsonPath.read<List<String>>(recentBody, "$.items[*].category").toSet())
        val recentIds = JsonPath.read<List<String>>(recentBody, "$.items[*].id")
        assertTrue(recentIds.containsAll(visible.slice(2..3).map { it.third }))
    }

    @DisplayName("최신 콘텐츠는 카테고리마다 가장 최근 글 하나를 고른다")
    @Test
    fun latestHighlightsPickNewestPerCategory() {
        // given
        val author = fixture.member()
        val olderArticle = fixture.article(today().minusDays(2), "${fixture.token} 이전 기사")
        val newerArticle = fixture.article(today().minusDays(1), "${fixture.token} 최근 기사")
        val event = fixture.event(today().plusDays(EVENT_DAYS_LEFT))
        val review = fixture.review(author, hidden = false)
        val comment = fixture.comment(author, review.product.id.toString(), deleted = false)
        val post = fixture.post(author, blinded = false)
        fixture.createdAt("webzine_articles", olderArticle.id, LATEST_BASE)
        fixture.createdAt("webzine_articles", newerArticle.id, LATEST_BASE.plusMinutes(1))
        fixture.createdAt("event_source_ref", event.id, LATEST_BASE)
        fixture.createdAt("product_reviews", review.id, LATEST_BASE)
        fixture.createdAt("content_comments", comment.id, LATEST_BASE.plusMinutes(1))
        fixture.createdAt("glow_community_posts", post.id, LATEST_BASE)

        // when
        val result = mockMvc.perform(get("/api/glow/feed/highlights").param("sort", "latest"))

        // then
        val body = bodyOf(result)
        assertEquals(listOf("trend", "news", "recent", "local"), JsonPath.read<List<String>>(body, "$[*].category"))
        assertEquals(
            listOf(
                "ARTICLE-${newerArticle.id}",
                "EVENT-${event.id}",
                "COMMENT-${comment.id}",
                "COMMUNITY_POST-${post.id}",
            ),
            JsonPath.read<List<String>>(body, "$[*].id"),
        )
    }

    @DisplayName("많이 본 콘텐츠는 카테고리마다 좋아요 수와 본 회원 수의 합이 가장 큰 글을 고른다")
    @Test
    fun popularHighlightsPickHighestScorePerCategory() {
        // given
        val author = fixture.member()
        val winnerArticle = fixture.article(today().minusDays(1), "${fixture.token} 인기 기사")
        val loserArticle = fixture.article(today().minusDays(1), "${fixture.token} 보통 기사")
        val winnerEvent = fixture.event(today().plusDays(EVENT_DAYS_LEFT))
        val loserEvent = fixture.event(today().plusDays(EVENT_DAYS_LEFT))
        val review = fixture.review(author, hidden = false)
        val comment = fixture.comment(author, UNVIEWED_PRODUCT_KEY, deleted = false)
        val winnerPost = fixture.post(author, blinded = false)
        fixture.post(author, blinded = false)
        fixture.react("ARTICLE", null, winnerArticle.key, count = 30)
        fixture.react(null, "ARTICLE", winnerArticle.key, count = 12)
        fixture.react("ARTICLE", null, loserArticle.key, count = 35)
        fixture.react(null, "EVENT_INFO", "TOUR_API:${winnerEvent.sourceId}", count = 25)
        fixture.react("EVENT_INFO", null, "TOUR_API:${loserEvent.sourceId}", count = 3)
        fixture.react("PRODUCT_REVIEW", null, review.id.toString(), count = 31)
        fixture.react("CONTENT_COMMENT", null, comment.id.toString(), count = 5)
        fixture.likePost(winnerPost.id, likeCount = WINNER_POST_LIKES)

        // when
        val result = mockMvc.perform(get("/api/glow/feed/highlights").param("sort", "popular"))

        // then
        val body = bodyOf(result)
        assertEquals(
            listOf(
                "ARTICLE-${winnerArticle.id}",
                "EVENT-${winnerEvent.id}",
                "PRODUCT_REVIEW-${review.id}",
                "COMMUNITY_POST-${winnerPost.id}",
            ),
            JsonPath.read<List<String>>(body, "$[*].id"),
        )
        assertEquals(30, JsonPath.read<Int>(body, "$[0].likeCount"))
        assertEquals(12, JsonPath.read<Int>(body, "$[0].viewerCount"))
        assertEquals(25, JsonPath.read<Int>(body, "$[1].viewerCount"))
        assertEquals(WINNER_POST_LIKES, JsonPath.read<Number>(body, "$[3].likeCount").toLong())
    }

    private fun bodyOf(result: ResultActions): String =
        result
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

    private fun today(): LocalDate = LocalDate.now(IdolGlowDateTimes.ZONE)

    companion object {
        private const val EVENT_DAYS_LEFT = 5L
        private const val WINNER_POST_LIKES = 5_000_000L
        private const val UNVIEWED_PRODUCT_KEY = "987654321"
        private val MERGE_BASE: LocalDateTime = LocalDateTime.of(2098, 1, 1, 0, 0)
        private val LATEST_BASE: LocalDateTime = LocalDateTime.of(2099, 1, 1, 0, 0)
    }
}
