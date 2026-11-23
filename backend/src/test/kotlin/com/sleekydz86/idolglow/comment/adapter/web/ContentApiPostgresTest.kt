package com.sleekydz86.idolglow.comment.adapter.web

import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.vo.Nickname
import com.sleekydz86.idolglow.user.user.infrastructure.UserJpaRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.util.UUID

@DisplayName("댓글·최근 본 콘텐츠 API")
class ContentApiPostgresTest : PostgresIntegrationTest() {
    private val json = MediaType.APPLICATION_JSON

    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .apply<DefaultMockMvcBuilder>(springSecurity())
                .build()
    }

    @DisplayName("댓글 목록은 로그인 없이 보고 작성은 로그인한 회원이 한다")
    @Test
    fun commentListPublicAndCreateRequiresLogin() {
        // given
        val me = member()
        val key = UUID.randomUUID().toString().take(8)
        val body =
            """{"targetType":"ARTICLE","targetKey":"$key","targetTitle":"기사",""" +
                """"targetPath":"/articles/$key","contentMarkdown":"댓글"}"""

        // when
        val anonymousCreate = mockMvc.perform(post("/api/comments").contentType(json).content(body))
        val created =
            mockMvc.perform(
                post("/api/comments").with(asMember(me)).contentType(json).content(body),
            )
        val list = mockMvc.perform(get("/api/comments").param("targetType", "ARTICLE").param("targetKey", key))

        // then
        anonymousCreate.andExpect(status().isUnauthorized)
        created.andExpect(status().isCreated).andExpect(jsonPath("$.depth").value(1))
        list
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].contentMarkdown").value("댓글"))
            .andExpect(jsonPath("$.items[0].authorName").value(me.nickname.value))
            .andExpect(jsonPath("$.items[0].replies").isArray)
    }

    @DisplayName("공지사항과 이벤트는 댓글과 최근 본 콘텐츠 대상이 아니다")
    @Test
    fun noticeAndEventRejected() {
        // given
        val me = member()
        val comment = """{"targetType":"NOTICE","targetKey":"1","contentMarkdown":"댓글"}"""
        val view = """{"contentType":"EVENT","contentKey":"1","title":"이벤트","path":"/events/1"}"""

        // when
        val commentResult =
            mockMvc.perform(
                post("/api/comments").with(asMember(me)).contentType(json).content(comment),
            )
        val viewResult =
            mockMvc.perform(
                post("/me/content-views").with(asMember(me)).contentType(json).content(view),
            )
        val listResult = mockMvc.perform(get("/api/comments").param("targetType", "EVENT").param("targetKey", "1"))

        // then
        commentResult.andExpect(status().isBadRequest)
        viewResult.andExpect(status().isBadRequest)
        listResult.andExpect(status().isBadRequest)
    }

    @DisplayName("댓글 이미지 업로드는 로그인이 필요하고 이미지가 아니면 거절한다")
    @Test
    fun imageUploadRequiresLoginAndImage() {
        // given
        val me = member()
        val file = MockMultipartFile("file", "note.txt", "text/plain", "hello".toByteArray())

        // when
        val anonymous = mockMvc.perform(multipart("/api/comments/images").file(file))
        val rejected = mockMvc.perform(multipart("/api/comments/images").file(file).with(asMember(me)))

        // then
        anonymous.andExpect(status().isUnauthorized)
        rejected
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("PNG, JPG, GIF, WEBP 이미지만 올릴 수 있습니다."))
    }

    @DisplayName("나의 리뷰와 최근 본 콘텐츠는 로그인이 필요하다")
    @Test
    fun meEndpointsRequireLogin() {
        // given
        val me = member()

        // when
        val anonymousReviews = mockMvc.perform(get("/me/reviews"))
        val reviews = mockMvc.perform(get("/me/reviews").with(user(me.id.toString()).roles("USER")))
        val views = mockMvc.perform(get("/me/content-views").with(user(me.id.toString()).roles("USER")))

        // then
        anonymousReviews.andExpect(status().isUnauthorized)
        reviews.andExpect(status().isOk).andExpect(jsonPath("$.totalElements").value(0))
        views.andExpect(status().isOk).andExpect(jsonPath("$.items").isArray)
    }

    private fun asMember(member: User) = user(member.id.toString()).roles("USER")

    private fun member(): User {
        val suffix = UUID.randomUUID().toString().replace("-", "").take(8)
        return userJpaRepository.save(User(email = "ca$suffix@test.com", nickname = Nickname.of("a$suffix")))
    }
}
