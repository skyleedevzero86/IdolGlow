package com.sleekydz86.idolglow.partner.adapter.web

import com.sleekydz86.idolglow.eventinfo.application.EventExternalIdService
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventMasterPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventTranslationPort
import com.sleekydz86.idolglow.global.infrastructure.exception.GlobalExceptionAdvice
import com.sleekydz86.idolglow.partner.application.PartnerAccessService
import com.sleekydz86.idolglow.partner.application.PartnerEventFeedService
import com.sleekydz86.idolglow.partner.application.PartnerRateLimiter
import com.sleekydz86.idolglow.partner.application.port.out.PartnerCallLogPort
import com.sleekydz86.idolglow.partner.application.port.out.PartnerClientPort
import com.sleekydz86.idolglow.partner.domain.PartnerCallLog
import com.sleekydz86.idolglow.partner.domain.PartnerClient
import com.sleekydz86.idolglow.partner.domain.PartnerKey
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@DisplayName("파트너 메타 API 인증·호출 제한·언어 계약")
class PartnerEventControllerTest {
    private val clientPort: PartnerClientPort = mockk()
    private val callLogs = mutableListOf<PartnerCallLog>()
    private val callLogPort: PartnerCallLogPort =
        mockk {
            val captured = slot<PartnerCallLog>()
            every { save(capture(captured)) } answers { captured.captured.also(callLogs::add) }
        }
    private val masterPort: EventMasterPort =
        mockk { every { findChangedAfter(any(), any(), any(), any()) } returns emptyList() }
    private val translationPort: EventTranslationPort = mockk { every { findByGlobalIdIn(any()) } returns emptyList() }
    private val externalIdService: EventExternalIdService = mockk { every { idsOf(any()) } returns emptyMap() }

    private val mockMvc: MockMvc =
        MockMvcBuilders
            .standaloneSetup(
                PartnerEventController(
                    PartnerEventFeedService(
                        PartnerAccessService(clientPort, callLogPort, PartnerRateLimiter()),
                        masterPort,
                        translationPort,
                        externalIdService,
                    ),
                ),
            ).setControllerAdvice(GlobalExceptionAdvice())
            .build()

    @DisplayName("P1 키 없이 부르면 401")
    @Test
    fun missingKeyIsUnauthorized() {
        // when
        val result = mockMvc.perform(get("/partner/v1/events"))

        // then
        result
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.errorCode").value("PARTNER_KEY_MISSING"))
        assertEquals(0, callLogs.size)
    }

    @DisplayName("P2 사용 중지된 키로 부르면 403이고 호출 기록을 남긴다")
    @Test
    fun disabledKeyIsForbidden() {
        // given
        등록(파트너(enabled = false))

        // when
        val result = mockMvc.perform(get("/partner/v1/events").header("X-Partner-Key", KEY))

        // then
        result
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("사용이 중지된 파트너 키입니다."))
        assertEquals(listOf(403), callLogs.map { it.status })
    }

    @DisplayName("P3 분당 한도를 넘기면 429와 Retry-After를 준다")
    @Test
    fun overLimitIsTooManyRequests() {
        // given
        등록(파트너(rateLimitPerMinute = 1))
        mockMvc.perform(get("/partner/v1/events").header("X-Partner-Key", KEY)).andExpect(status().isOk)

        // when
        val result = mockMvc.perform(get("/partner/v1/events").header("X-Partner-Key", KEY))

        // then
        result
            .andExpect(status().isTooManyRequests)
            .andExpect(header().exists("Retry-After"))
            .andExpect(jsonPath("$.errorCode").value("PARTNER_RATE_LIMITED"))
            .andExpect(jsonPath("$.name").value("PartnerExceptionType"))
        assertEquals(listOf(200, 429), callLogs.map { it.status })
    }

    @DisplayName("P7 계약하지 않은 언어를 요청하면 400")
    @Test
    fun uncontractedLocaleIsBadRequest() {
        // given
        등록(파트너())

        // when
        val result = mockMvc.perform(get("/partner/v1/events").header("X-Partner-Key", KEY).param("lang", "ja"))

        // then
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("PARTNER_LOCALE_NOT_CONTRACTED"))
        assertEquals(listOf(400), callLogs.map { it.status })
    }

    @DisplayName("모르는 외부 ID 체계로 공연을 찾으면 400이고 호출 기록을 남긴다")
    @Test
    fun unknownExternalIdSchemeIsBadRequest() {
        // given
        등록(파트너())

        // when
        val result =
            mockMvc.perform(
                get("/partner/v1/events/by-external-id")
                    .header("X-Partner-Key", KEY)
                    .param("scheme", "TMDB")
                    .param("id", "123"),
            )

        // then
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("알 수 없는 외부 ID 체계입니다. scheme=TMDB"))
        assertEquals(listOf(400), callLogs.map { it.status })
    }

    private fun 등록(client: PartnerClient) {
        every { clientPort.findByKeyHash(PartnerKey.hash(KEY)) } returns client
    }

    private fun 파트너(
        enabled: Boolean = true,
        rateLimitPerMinute: Int = 60,
    ): PartnerClient =
        PartnerClient(
            id = 7,
            name = "서울 여행사",
            keyHash = PartnerKey.hash(KEY),
            locales = "en",
            rateLimitPerMinute = rateLimitPerMinute,
            enabled = enabled,
        )

    companion object {
        private const val KEY: String = "pk_test-partner-key"
    }
}
