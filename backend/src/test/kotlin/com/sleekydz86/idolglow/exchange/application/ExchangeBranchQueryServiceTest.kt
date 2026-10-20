package com.sleekydz86.idolglow.exchange.application

import com.sleekydz86.idolglow.exchange.domain.ExchangeBranch
import com.sleekydz86.idolglow.exchange.infrastructure.ExchangeBranchJpaRepository
import com.sleekydz86.idolglow.exchange.infrastructure.NaverDirectionsClient
import com.sleekydz86.idolglow.exchangerate.application.port.out.ExchangeRateQueryPort
import com.sleekydz86.idolglow.exchangerate.domain.ExchangeRateQuote
import com.sleekydz86.idolglow.global.config.ExchangeAirportHubProperties
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("Exchange Branch Query Service")
class ExchangeBranchQueryServiceTest {
    private val exchangeBranchJpaRepository = mockk<ExchangeBranchJpaRepository>()
    private val exchangeRateQueryPort = mockk<ExchangeRateQueryPort>()
    private val naverDirectionsClient = mockk<NaverDirectionsClient>()
    private val exchangeAirportHubProperties =
        ExchangeAirportHubProperties(
            longitude = 126.4407,
            latitude = 37.4602,
        )

    private val service =
        ExchangeBranchQueryService(
            exchangeBranchJpaRepository = exchangeBranchJpaRepository,
            exchangeRateQueryPort = exchangeRateQueryPort,
            naverDirectionsClient = naverDirectionsClient,
            exchangeAirportHubProperties = exchangeAirportHubProperties,
        )

    @DisplayName("경로 조회 api 가 없으면 데모 이동 시간을 사용한다")
    @Test
    fun usesDemoTravelTimeWithoutRouteApi() {

        // given
        val jpyRows = 엔화_템플릿_지점목록()
        every { exchangeBranchJpaRepository.findByCurrencyOrderBySortOrderAsc("JPY") } returns jpyRows
        every { naverDirectionsClient.drivingDurationMinutes(any(), any(), any(), any()) } returns null

        // when
        val result = service.listBranchesWithDrivingMinutes("JPY")

        // then
        assertEquals(listOf(0, 90, 83, 83, 83), result.map { it.durationMinutesFromAirport })
    }

    @DisplayName("지점 데이터가 부족하면 템플릿 지점으로 보충한다")
    @Test
    fun fillsMissingBranchesFromTemplate() {

        // given
        val jpyRows = 엔화_템플릿_지점목록()
        every { exchangeBranchJpaRepository.findByCurrencyOrderBySortOrderAsc("JPY") } returns jpyRows
        every { exchangeBranchJpaRepository.findByCurrencyOrderBySortOrderAsc("CNY") } returns
            listOf(
                지점(
                    id = 11L,
                    name = "airport",
                    lat = 37.4602,
                    lng = 126.4407,
                    rate = "188.4",
                    currency = "CNY",
                    sortOrder = 0,
                    airportHub = true,
                ),
            )
        every { naverDirectionsClient.drivingDurationMinutes(any(), any(), any(), any()) } returns null

        // when
        val result = service.listBranchesWithDrivingMinutes("CNY")

        // then
        assertEquals(5, result.size)
        assertEquals(listOf("CNY", "CNY", "CNY", "CNY", "CNY"), result.map { it.currency })
        assertEquals(listOf(0, 90, 83, 83, 83), result.map { it.durationMinutesFromAirport })
    }

    @DisplayName("db 지점이 없으면 공식 환율로 전체 지점 목록을 생성한다")
    @Test
    fun buildsAllBranchesFromOfficialRates() {

        // given
        val jpyRows = 엔화_템플릿_지점목록()
        every { exchangeBranchJpaRepository.findByCurrencyOrderBySortOrderAsc("JPY") } returns jpyRows
        every { exchangeBranchJpaRepository.findByCurrencyOrderBySortOrderAsc("HKD") } returns emptyList()
        every { exchangeRateQueryPort.fetchDailyRates(null) } returns emptyList()
        every { exchangeRateQueryPort.fetchDailyRates(any<LocalDate>()) } answers {
            val date = invocation.args[0] as? LocalDate ?: return@answers emptyList()
            if (date == LocalDate.now().minusDays(1)) {
                listOf(
                    ExchangeRateQuote(
                        curUnit = "HKD",
                        curNm = "Hong Kong Dollar",
                        ttb = "188.00",
                        tts = "190.00",
                        dealBasR = "189.03",
                        bkpr = "189",
                    ),
                )
            } else {
                emptyList()
            }
        }
        every { naverDirectionsClient.drivingDurationMinutes(any(), any(), any(), any()) } returns null

        // when
        val result = service.listBranchesWithDrivingMinutes("HKD")

        // then
        assertEquals(5, result.size)
        assertEquals(listOf("HKD", "HKD", "HKD", "HKD", "HKD"), result.map { it.currency })
        assertEquals(BigDecimal("189.030000"), result.first().rate)
    }

    @DisplayName("경로 조회 api 결과가 있으면 해당 값을 사용한다")
    @Test
    fun usesRouteApiResultWhenPresent() {

        // given
        every { exchangeBranchJpaRepository.findByCurrencyOrderBySortOrderAsc("JPY") } returns
            listOf(
                지점(id = 2L, name = "branch", lat = 37.5635, lng = 126.9826, sortOrder = 1),
            )
        every { exchangeRateQueryPort.fetchDailyRates(null) } returns emptyList()
        every { exchangeRateQueryPort.fetchDailyRates(any<LocalDate>()) } returns emptyList()
        every {
            naverDirectionsClient.drivingDurationMinutes(
                startLng = 126.4407,
                startLat = 37.4602,
                goalLng = 126.9826,
                goalLat = 37.5635,
            )
        } returns 77

        // when
        val result = service.listBranchesWithDrivingMinutes("JPY")

        // then
        assertEquals(listOf(77), result.map { it.durationMinutesFromAirport })
    }

    private fun 엔화_템플릿_지점목록(): List<ExchangeBranch> =
        listOf(
            지점(
                id = 1L,
                name = "airport",
                lat = 37.4602,
                lng = 126.4407,
                rate = "9.33",
                sortOrder = 0,
                airportHub = true,
            ),
            지점(id = 2L, name = "myeongdong-a", lat = 37.5635, lng = 126.9826, rate = "9.23", sortOrder = 1),
            지점(id = 3L, name = "myeongdong-b", lat = 37.5640, lng = 126.9815, rate = "9.23", sortOrder = 2),
            지점(id = 4L, name = "myeongdong-c", lat = 37.5632, lng = 126.9850, rate = "9.23", sortOrder = 3),
            지점(id = 5L, name = "myeongdong-d", lat = 37.5628, lng = 126.9838, rate = "9.2283", sortOrder = 4),
        )

    private fun 지점(
        id: Long,
        name: String,
        lat: Double,
        lng: Double,
        rate: String = "9.23",
        currency: String = "JPY",
        sortOrder: Int,
        airportHub: Boolean = false,
    ): ExchangeBranch =
        ExchangeBranch(
            id = id,
            name = name,
            rate = BigDecimal(rate),
            currency = currency,
            lat = lat,
            lng = lng,
            sortOrder = sortOrder,
            airportHub = airportHub,
        )
}
