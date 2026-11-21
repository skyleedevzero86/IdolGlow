package com.sleekydz86.idolglow.eventinfo.adapter.web

import com.sleekydz86.idolglow.eventinfo.application.EventMetaProjectionService
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaDeadLetter
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaMessage
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaSnapshot
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventMetaDeadLetterJpaRepository
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@DisplayName("관리자 공연 메타 실패 메시지 API")
class AdminEventMetaDeadLetterApiPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var context: WebApplicationContext

    @Autowired private lateinit var eventMetaDeadLetterJpaRepository: EventMetaDeadLetterJpaRepository

    @Autowired private lateinit var objectMapper: ObjectMapper

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var mockMvc: MockMvc

    private val asAdmin = user("1").roles("ADMIN")

    private val asMember = user("2").roles("USER")

    @BeforeEach
    fun setUp() {
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .apply<DefaultMockMvcBuilder>(springSecurity())
                .build()
    }

    @AfterEach
    fun clean() {
        jdbcTemplate.update("delete from event_meta_dead_letter")
        jdbcTemplate.update("delete from consumed_event")
        jdbcTemplate.update("delete from event_meta_projection")
    }

    @DisplayName("목록은 관리자만 볼 수 있다")
    @Test
    fun listIsAdminOnly() {
        // given
        val saved = 실패_메시지()

        // when
        val anonymous = mockMvc.perform(get(PATH))
        val member = mockMvc.perform(get(PATH).with(asMember))
        val admin = mockMvc.perform(get(PATH).with(asAdmin))

        // then
        anonymous.andExpect(status().isUnauthorized)
        member.andExpect(status().isForbidden)
        admin
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(saved.id))
            .andExpect(jsonPath("$[0].consumerName").value(EventMetaProjectionService.CONSUMER))
    }

    @DisplayName("다시 처리하면 반영되고, 같은 건을 또 처리하면 409, 없는 건은 404")
    @Test
    fun reprocessOnceThenConflict() {
        // given
        val saved = 실패_메시지()

        // when
        val first = mockMvc.perform(post("$PATH/${saved.id}/reprocess").with(asAdmin))
        val again = mockMvc.perform(post("$PATH/${saved.id}/reprocess").with(asAdmin))
        val missing = mockMvc.perform(post("$PATH/999999/reprocess").with(asAdmin))
        val byMember = mockMvc.perform(post("$PATH/${saved.id}/reprocess").with(asMember))

        // then
        first
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result").value("APPLIED"))
            .andExpect(jsonPath("$.deadLetter.reprocessedAt").isNotEmpty)
        again
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.errorCode").value("EVENT_META_DEAD_LETTER_ALREADY_REPROCESSED"))
            .andExpect(jsonPath("$.message").value("이미 다시 처리한 메시지입니다."))
        missing.andExpect(status().isNotFound)
        byMember.andExpect(status().isForbidden)
    }

    private fun 실패_메시지(): EventMetaDeadLetter {
        val message =
            EventMetaMessage(
                eventId = UUID.randomUUID().toString(),
                eventType = "UPDATED",
                globalId = UUID.randomUUID().toString(),
                metaVersion = 1,
                occurredAt = "2026-11-21T09:00:00+09:00",
                changedFields = listOf("venue"),
                snapshot =
                    EventMetaSnapshot(
                        title = "실패 공연",
                        startDate = LocalDate.of(2026, 12, 1),
                        endDate = LocalDate.of(2026, 12, 3),
                        venue = "세종문화회관",
                        status = "ACTIVE",
                    ),
            )
        return eventMetaDeadLetterJpaRepository.save(
            EventMetaDeadLetter(
                consumerName = EventMetaProjectionService.CONSUMER,
                eventId = message.eventId,
                payload = objectMapper.writeValueAsString(message),
                errorType = "org.springframework.dao.CannotAcquireLockException",
                failedAt = LocalDateTime.now(),
            ),
        )
    }

    companion object {
        private const val PATH: String = "/api/admin/event-meta/dead-letters"
    }
}
