package com.sleekydz86.idolglow.eventinfo.adapter.web

import com.sleekydz86.idolglow.eventinfo.domain.EventMaster
import com.sleekydz86.idolglow.eventinfo.domain.EventMasterStatus
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventMasterJpaRepository
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDate
import java.util.UUID

@DisplayName("관리자 공연 외부 ID API")
class AdminEventExternalIdApiPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var context: WebApplicationContext

    @Autowired private lateinit var eventMasterJpaRepository: EventMasterJpaRepository

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var mockMvc: MockMvc

    private val globalId: String = UUID.randomUUID().toString()

    @BeforeEach
    fun setUp() {
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .apply<DefaultMockMvcBuilder>(springSecurity())
                .build()
        eventMasterJpaRepository.save(
            EventMaster(
                globalId = globalId,
                title = "외부 번호 공연",
                normalizedTitle = "외부번호공연${globalId.take(8)}",
                startDate = LocalDate.of(2026, 12, 1),
                endDate = LocalDate.of(2026, 12, 3),
                venue = "세종문화회관",
                normalizedVenue = "세종문화회관",
                status = EventMasterStatus.ACTIVE,
            ),
        )
    }

    @AfterEach
    fun clean() {
        jdbcTemplate.update("delete from event_external_id where global_id = ?", globalId)
    }

    @DisplayName("관리자만 위키데이터 번호를 붙이고 뗄 수 있다")
    @Test
    fun onlyAdminCanAttachAndDetach() {
        // given
        val path = "/api/admin/events/$globalId/external-ids"
        val body = """{"scheme":"WIKIDATA","externalId":"Q4242"}"""

        // when
        val request = { post(path).contentType(MediaType.APPLICATION_JSON).content(body) }
        val anonymous = mockMvc.perform(request())
        val member = mockMvc.perform(request().with(user("2").roles("USER")))
        val attached = mockMvc.perform(request().with(user("1").roles("ADMIN")))
        val listed = mockMvc.perform(get(path).with(user("1").roles("ADMIN")))
        val detached = mockMvc.perform(delete("$path/WIKIDATA/Q4242").with(user("1").roles("ADMIN")))
        val detachedAgain = mockMvc.perform(delete("$path/WIKIDATA/Q4242").with(user("1").roles("ADMIN")))

        // then
        anonymous.andExpect(status().isUnauthorized)
        member.andExpect(status().isForbidden)
        attached.andExpect(status().isCreated).andExpect(jsonPath("$.id").value("Q4242"))
        listed.andExpect(status().isOk).andExpect(jsonPath("$[0].scheme").value("WIKIDATA"))
        detached.andExpect(status().isNoContent)
        detachedAgain
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.errorCode").value("EVENT_EXTERNAL_ID_NOT_LINKED"))
    }
}
