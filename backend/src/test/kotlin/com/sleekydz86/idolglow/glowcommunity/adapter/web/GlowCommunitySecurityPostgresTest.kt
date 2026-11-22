package com.sleekydz86.idolglow.glowcommunity.adapter.web

import com.sleekydz86.idolglow.glowcommunity.application.dto.CreateGlowCommunityPostCommand
import com.sleekydz86.idolglow.glowcommunity.application.port.`in`.GlowCommunityPostCommandUseCase
import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunityTag
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.vo.Nickname
import com.sleekydz86.idolglow.user.user.infrastructure.UserJpaRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.util.UUID

@DisplayName("GLOW 지역사회 API 권한")
class GlowCommunitySecurityPostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired
    private lateinit var commandUseCase: GlowCommunityPostCommandUseCase

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .apply<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(springSecurity())
                .build()
    }

    @DisplayName("목록과 하이라이트와 추천 검색어는 로그인 없이 볼 수 있다")
    @Test
    fun publicReadWithoutLogin() {
        // given
        val author = member()
        val token = "s" + UUID.randomUUID().toString().take(8)
        commandUseCase.create(CreateGlowCommunityPostCommand(author.id, GlowCommunityTag.TALK, "$token 공개", "본문"))

        // when
        val list = mockMvc.perform(get("/api/glow-community/posts").param("q", token).param("tag", "talk"))
        val highlights = mockMvc.perform(get("/api/glow-community/highlights"))
        val keywords = mockMvc.perform(get("/api/glow-community/keywords"))

        // then
        list
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].tag").value("talk"))
            .andExpect(jsonPath("$.items[0].liked").value(false))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.hasNext").value(false))
        highlights.andExpect(status().isOk)
        keywords.andExpect(status().isOk).andExpect(jsonPath("$.keywords").isArray)
    }

    @DisplayName("글쓰기는 로그인이 필요하다")
    @Test
    fun writeRequiresLogin() {
        // given
        val body = """{"tag":"talk","title":"제목","content":"본문"}"""

        // when
        val result =
            mockMvc.perform(post("/api/glow-community/posts").contentType(MediaType.APPLICATION_JSON).content(body))

        // then
        result.andExpect(status().isUnauthorized)
    }

    @DisplayName("블라인드와 해제는 관리자만 할 수 있다")
    @Test
    fun blindRequiresAdmin() {
        // given
        val author = member()
        val command = CreateGlowCommunityPostCommand(author.id, GlowCommunityTag.TALK, "권한 확인", "본문")
        val post = commandUseCase.create(command)
        val blindPath = "/api/admin/glow-community/posts/${post.id}/blind"
        val unblindPath = "/api/admin/glow-community/posts/${post.id}/unblind"
        val reportsPath = "/api/admin/glow-community/reports"
        val member = member()

        // when
        val asMember = user(member.id.toString()).roles("USER")
        val asAdmin = user("1").roles("ADMIN")
        val anonymous = mockMvc.perform(post(blindPath))
        val byMember = mockMvc.perform(post(blindPath).with(asMember))
        val byAdmin = mockMvc.perform(post(blindPath).with(asAdmin))
        val unblindByMember = mockMvc.perform(post(unblindPath).with(asMember))
        val unblindByAdmin = mockMvc.perform(post(unblindPath).with(asAdmin))
        val reports = mockMvc.perform(get(reportsPath).with(asAdmin))
        val reportsByMember = mockMvc.perform(get(reportsPath).with(asMember))

        // then
        anonymous.andExpect(status().isUnauthorized)
        byMember.andExpect(status().isForbidden)
        byAdmin.andExpect(status().isOk).andExpect(jsonPath("$.blinded").value(true))
        unblindByMember.andExpect(status().isForbidden)
        unblindByAdmin.andExpect(status().isOk).andExpect(jsonPath("$.blinded").value(false))
        reports.andExpect(status().isOk).andExpect(jsonPath("$.page").value(0))
        reportsByMember.andExpect(status().isForbidden)
    }

    private fun member(): User {
        val suffix = UUID.randomUUID().toString().replace("-", "").take(8)
        return userJpaRepository.save(User(email = "sc$suffix@test.com", nickname = Nickname.of("s$suffix")))
    }
}
