package com.sleekydz86.idolglow.eventinfo.adapter.web

import com.sleekydz86.idolglow.eventinfo.application.EventTranslationTxService
import com.sleekydz86.idolglow.eventinfo.domain.EventMaster
import com.sleekydz86.idolglow.eventinfo.domain.EventMasterStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslationOrigin
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventMasterJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventTranslationJpaRepository
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDate
import java.util.UUID

@DisplayName("관리자 공연 사람 번역 저장 API")
class AdminEventTranslationApiPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var context: WebApplicationContext

    @Autowired private lateinit var eventMasterJpaRepository: EventMasterJpaRepository

    @Autowired private lateinit var eventTranslationJpaRepository: EventTranslationJpaRepository

    @Autowired private lateinit var eventTranslationTxService: EventTranslationTxService

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
                title = "서울빛초롱축제",
                normalizedTitle = "서울빛초롱축제${globalId.take(8)}",
                startDate = LocalDate.of(2026, 12, 1),
                endDate = LocalDate.of(2026, 12, 3),
                venue = null,
                normalizedVenue = null,
                status = EventMasterStatus.ACTIVE,
            ),
        )
    }

    @AfterEach
    fun clean() {
        jdbcTemplate.update("delete from event_translation where global_id = ?", globalId)
        jdbcTemplate.update("delete from event_outbox where aggregate_id = ?", globalId)
    }

    private fun save(
        title: String,
        locale: String = "ja",
        target: String = globalId,
        role: String? = "ADMIN",
    ): ResultActions {
        val body = """{"title":"${title.replace("\"", "\\\"")}"}"""
        val request =
            put("/api/admin/events/$target/translations/$locale")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        return mockMvc.perform(if (role == null) request else request.with(user("1").roles(role)))
    }

    @DisplayName("관리자가 저장하면 공식 번역이 있어도 사람 번역으로 바뀐다")
    @Test
    fun adminOverwritesProviderWithManual() {
        // given
        eventTranslationTxService.upsertProvider(globalId, "ja", "ソウル光初籠祭り", null)

        // when
        val result = save("ソウル灯籠祭り（서울빛초롱축제）")

        // then
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.origin").value("MANUAL"))
            .andExpect(jsonPath("$.title").value("ソウル灯籠祭り（서울빛초롱축제）"))
        val stored = eventTranslationJpaRepository.findByGlobalIdAndLocale(globalId, "ja")!!
        assertEquals(EventTranslationOrigin.MANUAL, stored.origin)
        assertEquals("ソウル灯籠祭り（서울빛초롱축제）", stored.title)
    }

    @DisplayName("로그인하지 않으면 401, 일반 회원이면 403이고 저장되지 않는다")
    @Test
    fun rejectsAnonymousAndMember() {
        // when
        val anonymous = save("首尔灯笼节", locale = "zh", role = null)
        val member = save("首尔灯笼节", locale = "zh", role = "USER")

        // then
        anonymous.andExpect(status().isUnauthorized)
        member.andExpect(status().isForbidden)
        assertNull(eventTranslationJpaRepository.findByGlobalIdAndLocale(globalId, "zh"))
    }

    @DisplayName("없는 대표 공연이면 404")
    @Test
    fun rejectsUnknownEvent() {
        // when
        val result = save("Seoul Lantern Festival", locale = "en", target = UUID.randomUUID().toString())

        // then
        result.andExpect(status().isNotFound).andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
    }

    @DisplayName("대상 언어가 아니면 400이고 저장되지 않는다")
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = ["ko", "fr"])
    fun rejectsNonTargetLocale(locale: String) {
        // when
        val result = save("Seoul Lantern Festival", locale = locale)

        // then
        result.andExpect(status().isBadRequest).andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"))
        assertNull(eventTranslationJpaRepository.findByGlobalIdAndLocale(globalId, locale))
    }

    @DisplayName("빈 제목과 공백만 있는 제목은 400")
    @ParameterizedTest(name = "[{0}]")
    @ValueSource(strings = ["", "   ", " "])
    fun rejectsBlankTitle(title: String) {
        // when
        val result = save(title, locale = "en")

        // then
        result.andExpect(status().isBadRequest).andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"))
        assertNull(eventTranslationJpaRepository.findByGlobalIdAndLocale(globalId, "en"))
    }

    @DisplayName("제목은 문자 수 255까지 저장되고 256이면 400이다")
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = ["灯", "祭", "😀", "가"])
    fun titleLengthBoundary(unit: String) {
        // when
        val atLimit = save(unit.repeat(255), locale = "zh")
        val overLimit = save(unit.repeat(256), locale = "ja")

        // then
        atLimit.andExpect(status().isOk)
        assertEquals(unit.repeat(255), eventTranslationJpaRepository.findByGlobalIdAndLocale(globalId, "zh")!!.title)
        overLimit.andExpect(status().isBadRequest)
        assertNull(eventTranslationJpaRepository.findByGlobalIdAndLocale(globalId, "ja"))
    }
}
