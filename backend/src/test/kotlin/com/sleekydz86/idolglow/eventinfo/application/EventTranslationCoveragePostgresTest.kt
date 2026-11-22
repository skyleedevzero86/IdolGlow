package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.adapter.mcp.EventTranslationCoverageMcpTools
import com.sleekydz86.idolglow.eventinfo.domain.EventMaster
import com.sleekydz86.idolglow.eventinfo.domain.EventMasterStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslation
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslationOrigin
import com.sleekydz86.idolglow.eventinfo.domain.MissingTranslation
import com.sleekydz86.idolglow.eventinfo.domain.TranslationCoverage
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventCollectLogJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventMasterJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventSourceRefJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventTranslationJpaRepository
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import io.micrometer.core.instrument.MeterRegistry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

@DisplayName("공연 번역 누락 현황")
class EventTranslationCoveragePostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var coverageService: EventTranslationCoverageService

    @Autowired
    private lateinit var coverageMetrics: EventTranslationCoverageMetrics

    @Autowired
    private lateinit var coverageMcpTools: EventTranslationCoverageMcpTools

    @Autowired
    private lateinit var meterRegistry: MeterRegistry

    @Autowired
    private lateinit var eventMasterJpaRepository: EventMasterJpaRepository

    @Autowired
    private lateinit var eventTranslationJpaRepository: EventTranslationJpaRepository

    @Autowired
    private lateinit var eventSourceRefJpaRepository: EventSourceRefJpaRepository

    @Autowired
    private lateinit var eventCollectLogJpaRepository: EventCollectLogJpaRepository

    @BeforeEach
    fun clean() {
        eventTranslationJpaRepository.deleteAllInBatch()
        eventSourceRefJpaRepository.deleteAllInBatch()
        eventCollectLogJpaRepository.deleteAllInBatch()
        eventMasterJpaRepository.deleteAllInBatch()
    }

    @AfterEach
    fun clearSecurity() {
        SecurityContextHolder.clearContext()
    }

    @DisplayName("공급처 번역, 기계 번역만, 번역 없음을 각각 센다")
    @Test
    fun countsVisibleMachineOnlyAndMissingSeparately() {
        // given
        master("g-a", "가 공연")
        master("g-b", "나 공연")
        master("g-c", "다 공연")
        translation("g-a", "en", EventTranslationOrigin.PROVIDER)
        translation("g-b", "en", EventTranslationOrigin.MACHINE)

        // when
        val en = coverageService.coverage().at("en")

        // then
        assertEquals(3L, en.totalActive)
        assertEquals(1L, en.visible)
        assertEquals(1L, en.machineOnly)
        assertEquals(1L, en.missing)
        assertEquals(listOf(MissingTranslation("g-c", "다 공연")), en.missingSamples)
        assertTrue(en.isConsistent())
    }

    @DisplayName("기계 번역을 운영자가 고치면 기계 번역만에서 보이는 번역으로 옮겨 간다")
    @Test
    fun manualFixMovesMachineOnlyToVisible() {
        // given
        master("g-a", "가 공연")
        val machine = translation("g-a", "en", EventTranslationOrigin.MACHINE)
        val before = coverageService.coverage().at("en")

        // when
        eventTranslationJpaRepository.delete(machine)
        eventTranslationJpaRepository.flush()
        translation("g-a", "en", EventTranslationOrigin.MANUAL)
        val after = coverageService.coverage().at("en")

        // then
        assertEquals(0L, before.visible)
        assertEquals(1L, before.machineOnly)
        assertEquals(1L, after.visible)
        assertEquals(0L, after.machineOnly)
        assertEquals(0L, after.missing)
        assertEquals(1, eventTranslationJpaRepository.findByGlobalId("g-a").size)
    }

    @DisplayName("ja와 zh는 언어마다 따로 계산한다")
    @Test
    fun eachLocaleIsCountedSeparately() {
        // given
        master("g-a", "가 공연")
        master("g-b", "나 공연")
        translation("g-a", "ja", EventTranslationOrigin.PROVIDER)
        translation("g-b", "ja", EventTranslationOrigin.MANUAL)
        translation("g-a", "zh", EventTranslationOrigin.MACHINE)

        // when
        val result = coverageService.coverage()
        val ja = result.at("ja")
        val zh = result.at("zh")
        val en = result.at("en")

        // then
        assertEquals(listOf("en", "ja", "zh"), result.map { it.locale })
        assertEquals(2L, ja.visible)
        assertEquals(0L, ja.missing)
        assertEquals(0L, zh.visible)
        assertEquals(1L, zh.machineOnly)
        assertEquals(1L, zh.missing)
        assertEquals(listOf("g-b"), zh.missingSamples.map { it.globalId })
        assertEquals(2L, en.missing)
        assertEquals(listOf("g-a", "g-b"), en.missingSamples.map { it.globalId })
    }

    @DisplayName("행사가 없으면 모두 0이다")
    @Test
    fun noEventsGivesZeros() {
        // given
        val emptyEvents = 0L

        // when
        val result = coverageService.coverage()

        // then
        assertEquals(3, result.size)
        result.forEach { coverage ->
            assertEquals(emptyEvents, coverage.totalActive)
            assertEquals(0L, coverage.visible)
            assertEquals(0L, coverage.machineOnly)
            assertEquals(0L, coverage.missing)
            assertTrue(coverage.missingSamples.isEmpty())
        }
    }

    @DisplayName("샘플 수는 요청한 만큼만 globalId 순서로 돌려준다")
    @Test
    fun samplesAreLimitedAndOrdered() {
        // given
        master("g-c", "다 공연")
        master("g-a", "가 공연")
        master("g-b", "나 공연")

        // when
        val en = coverageService.coverage(sampleLimit = 2).at("en")
        val none = coverageService.coverage(sampleLimit = 0).at("en")

        // then
        assertEquals(3L, en.missing)
        assertEquals(listOf("g-a", "g-b"), en.missingSamples.map { it.globalId })
        assertEquals(3L, none.missing)
        assertTrue(none.missingSamples.isEmpty())
    }

    @DisplayName("지표는 갱신할 때만 계산한 값을 보여 준다")
    @Test
    fun gaugesShowRefreshedValues() {
        // given
        master("g-a", "가 공연")
        master("g-b", "나 공연")
        translation("g-a", "en", EventTranslationOrigin.MACHINE)

        // when
        coverageMetrics.refresh()

        // then
        assertEquals(1.0, gauge(EventTranslationCoverageMetrics.MISSING_METRIC, "en"))
        assertEquals(1.0, gauge(EventTranslationCoverageMetrics.MACHINE_ONLY_METRIC, "en"))
        assertEquals(2.0, gauge(EventTranslationCoverageMetrics.MISSING_METRIC, "ja"))
        assertEquals(0.0, gauge(EventTranslationCoverageMetrics.MACHINE_ONLY_METRIC, "zh"))
    }

    @DisplayName("MCP 도구는 관리자만 쓸 수 있다")
    @Test
    fun mcpToolRequiresAdmin() {
        // given
        master("g-a", "가 공연")

        // when
        authenticate("USER")
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                coverageMcpTools.translationCoverage(null)
            }
        authenticate("ADMIN")
        val result = coverageMcpTools.translationCoverage(null)

        // then
        assertTrue(error.message?.contains("번역 현황 조회 권한이 없습니다") == true)
        assertEquals(listOf(1L, 1L, 1L), result.map { it.missing })
    }

    private fun gauge(
        name: String,
        locale: String,
    ): Double =
        meterRegistry
            .get(name)
            .tag("locale", locale)
            .gauge()
            .value()

    private fun authenticate(role: String) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken("1", "", listOf(SimpleGrantedAuthority("ROLE_$role")))
    }

    private fun master(
        globalId: String,
        title: String,
    ): EventMaster =
        eventMasterJpaRepository.saveAndFlush(
            EventMaster(
                globalId = globalId,
                title = title,
                normalizedTitle = title,
                startDate = null,
                endDate = null,
                venue = null,
                normalizedVenue = null,
                status = EventMasterStatus.ACTIVE,
            ),
        )

    private fun translation(
        globalId: String,
        locale: String,
        origin: EventTranslationOrigin,
    ): EventTranslation =
        eventTranslationJpaRepository.saveAndFlush(
            EventTranslation(
                globalId = globalId,
                locale = locale,
                title = "$globalId-$locale",
                synopsis = null,
                origin = origin,
            ),
        )

    private fun List<TranslationCoverage>.at(locale: String): TranslationCoverage = single { it.locale == locale }
}
