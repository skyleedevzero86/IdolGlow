package com.sleekydz86.idolglow.webzine.adapter.web

import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.webzine.domain.IssueCategory
import com.sleekydz86.idolglow.webzine.domain.WebzineArticle
import com.sleekydz86.idolglow.webzine.domain.WebzineArticleDraft
import com.sleekydz86.idolglow.webzine.domain.WebzineArticleSectionDraft
import com.sleekydz86.idolglow.webzine.domain.WebzineIssue
import com.sleekydz86.idolglow.webzine.domain.WebzineIssueRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random

@DisplayName("웹진 공개 조회 API")
class WebzinePublicPostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired
    private lateinit var webzineIssueRepository: WebzineIssueRepository

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    private lateinit var mockMvc: MockMvc

    private val createdSlugs = mutableListOf<String>()
    private val baseVolume = VOLUME_BASE + Random.nextInt(VOLUME_RANGE) * VOLUME_STEP
    private val token = "wz" + UUID.randomUUID().toString().take(8)

    @BeforeEach
    fun setUp() {
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .apply<DefaultMockMvcBuilder>(springSecurity())
                .build()
    }

    @AfterEach
    fun tearDown() {
        transactionTemplate.executeWithoutResult {
            createdSlugs.mapNotNull(webzineIssueRepository::findBySlug).forEach(webzineIssueRepository::delete)
        }
    }

    @DisplayName("비회원도 발행된 호 목록과 찾기 조건을 볼 수 있고 예약·빈 호는 빠진다")
    @Test
    fun guestSeesOnlyPublishedIssues() {
        // given
        val published = seedIssue(baseVolume, LocalDate.of(2001, 3, 1), articleTitles = listOf("$token 첫 기사", "두 번째 기사"))
        val scheduled = seedIssue(baseVolume + 1, LocalDate.now(IdolGlowDateTimes.ZONE).plusMonths(2), articleTitles = listOf("예약 기사"))
        val empty = seedIssue(baseVolume + 2, LocalDate.of(2001, 4, 1), articleTitles = emptyList())

        // when
        val list = mockMvc.perform(get("/api/webzine/issues").param("keyword", token))
        val byArticleTitle = mockMvc.perform(get("/api/webzine/issues").param("keyword", "$token 첫"))
        val filters = mockMvc.perform(get("/api/webzine/issues/filters"))

        // then
        list
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.items[0].slug").value(published.slug))
            .andExpect(jsonPath("$.items[0].volume").value(published.volume))
            .andExpect(jsonPath("$.items[0].issueDate").value("2001.03."))
            .andExpect(jsonPath("$.items[0].articleCount").value(2))
            .andExpect(jsonPath("$.items[0].articles.length()").value(2))
            .andExpect(jsonPath("$.items[0].leadArticleSlug").isNotEmpty)
        byArticleTitle.andExpect(status().isOk).andExpect(jsonPath("$.items[0].slug").value(published.slug))
        filters
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.years").isArray)
            .andExpect(jsonPath("$.months['2001']").isArray)
            .andExpect(jsonPath("$.volumes[?(@.volume == %d)]", published.volume).isNotEmpty)
            .andExpect(jsonPath("$.volumes[?(@.volume == %d)]", scheduled.volume).isEmpty)
            .andExpect(jsonPath("$.volumes[?(@.volume == %d)]", empty.volume).isEmpty)
    }

    @DisplayName("목록은 0부터 시작하는 페이지와 연도·월·호 조건으로 거를 수 있다")
    @Test
    fun pagingAndFilters() {
        // given
        val first = seedIssue(baseVolume, LocalDate.of(2001, 1, 1), articleTitles = listOf("1월 기사"))
        val second = seedIssue(baseVolume + 1, LocalDate.of(2001, 2, 1), articleTitles = listOf("2월 기사"))
        val third = seedIssue(baseVolume + 2, LocalDate.of(2002, 2, 1), articleTitles = listOf("이듬해 기사"))

        // when
        val firstPage = mockMvc.perform(get("/api/webzine/issues").param("keyword", token).param("size", "2"))
        val secondPage = mockMvc.perform(get("/api/webzine/issues").param("keyword", token).param("size", "2").param("page", "1"))
        val byYearMonth = mockMvc.perform(get("/api/webzine/issues").param("keyword", token).param("year", "2001").param("month", "2"))
        val byVolume = mockMvc.perform(get("/api/webzine/issues").param("keyword", token).param("volume", first.volume.toString()))

        // then
        firstPage
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.items[0].volume").value(third.volume))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.hasNext").value(true))
        secondPage
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].volume").value(first.volume))
            .andExpect(jsonPath("$.hasNext").value(false))
        byYearMonth.andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.items[0].volume").value(second.volume))
        byVolume.andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.items[0].slug").value(first.slug))
    }

    @DisplayName("발행된 호와 기사 상세는 비회원도 볼 수 있고 관리자 전용 정보는 내려주지 않는다")
    @Test
    fun guestReadsPublishedDetail() {
        // given
        val issue = seedIssue(baseVolume, LocalDate.of(2001, 5, 1), articleTitles = listOf("상세 기사"))
        val articleSlug = "$token-article-0"

        // when
        val detail = mockMvc.perform(get("/api/webzine/issues/${issue.slug}"))
        val article = mockMvc.perform(get("/api/webzine/issues/${issue.slug}/articles/$articleSlug"))

        // then
        detail
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.volume").value(issue.volume))
            .andExpect(jsonPath("$.articles[0].slug").value(articleSlug))
            .andExpect(jsonPath("$.articles[0].order").value(0))
            .andExpect(jsonPath("$.articles[0].categoryLabel").value(IssueCategory.ARTICLE.label))
        article
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("상세 기사"))
            .andExpect(jsonPath("$.issueSlug").value(issue.slug))
            .andExpect(jsonPath("$.authorName").value("기자"))
            .andExpect(jsonPath("$.authorEmail").doesNotExist())
            .andExpect(jsonPath("$.sections[0].paragraphs.length()").value(2))
            .andExpect(jsonPath("$.tags[0]").value("트렌드"))
            .andExpect(jsonPath("$.galleryImageUrls").isArray)
    }

    @DisplayName("예약 호·기사 없는 호·없는 기사는 비회원에게 401 이 아닌 404 를 돌려준다")
    @Test
    fun unpublishedReturnsNotFound() {
        // given
        val scheduled = seedIssue(baseVolume, LocalDate.now(IdolGlowDateTimes.ZONE).plusMonths(1), articleTitles = listOf("예약 기사"))
        val empty = seedIssue(baseVolume + 1, LocalDate.of(2001, 6, 1), articleTitles = emptyList())
        val published = seedIssue(baseVolume + 2, LocalDate.of(2001, 7, 1), articleTitles = listOf("공개 기사"))
        val paths =
            listOf(
                "/api/webzine/issues/${scheduled.slug}",
                "/api/webzine/issues/${scheduled.slug}/articles/$token-article-0",
                "/api/webzine/issues/${empty.slug}",
                "/api/webzine/issues/${published.slug}/articles/missing-article",
                "/api/webzine/issues/vol-0-missing",
            )

        // when
        val statuses = paths.associateWith { path -> mockMvc.perform(get(path)).andReturn().response.status }

        // then
        statuses.forEach { (path, status) -> assertEquals(NOT_FOUND, status, path) }
    }

    private fun seedIssue(
        volume: Int,
        issueDate: LocalDate,
        articleTitles: List<String>,
    ): WebzineIssue {
        val slug = "vol-$volume"
        createdSlugs += slug
        return transactionTemplate.execute {
            val issue =
                WebzineIssue.create(
                    slug = slug,
                    volume = volume,
                    issueDate = issueDate,
                    coverImageUrl = "https://example.com/cover-$volume.jpg",
                    teaser = "$token 소개 문구",
                )
            articleTitles.forEachIndexed { index, title ->
                issue.addArticle(WebzineArticle.create(issue, "$token-article-$index", draft(title)))
            }
            webzineIssueRepository.save(issue)
        }!!
    }

    private fun draft(title: String): WebzineArticleDraft =
        WebzineArticleDraft(
            title = title,
            kicker = "부제",
            summary = "요약",
            heroImageUrl = "https://example.com/hero.jpg",
            cardImageUrl = "https://example.com/card.jpg",
            category = IssueCategory.ARTICLE,
            formatLabel = "아티클",
            authorName = "기자",
            authorEmail = "writer@example.com",
            creditLine = "크레딧",
            highlightQuote = null,
            sections = listOf(WebzineArticleSectionDraft(heading = "소제목", body = "첫 문단\n\n둘째 문단", note = null)),
            galleryImageUrls = listOf("https://example.com/hero.jpg"),
            tags = listOf("트렌드"),
        )

    companion object {
        private const val NOT_FOUND = 404
        private const val VOLUME_BASE = 500_000
        private const val VOLUME_RANGE = 100_000
        private const val VOLUME_STEP = 10
    }
}
