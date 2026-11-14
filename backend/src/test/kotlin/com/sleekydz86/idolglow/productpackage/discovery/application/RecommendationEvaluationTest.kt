package com.sleekydz86.idolglow.productpackage.discovery.application

import com.sleekydz86.idolglow.productpackage.discovery.application.dto.ProductRankingResponse
import com.sleekydz86.idolglow.productpackage.option.domain.Option
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.product.domain.ProductLocation
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.user.user.domain.vo.ConceptType
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Transactional
@DisplayName("설문 기반 추천 평가 (고정 데이터)")
class RecommendationEvaluationTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var productDiscoveryService: ProductDiscoveryService

    @Test
    @DisplayName("설문 4종(컨셉만·장소만·기간만·전부)의 상위 3개와 추천 이유가 기대 표와 같다")
    fun surveyVariantsMatchExpectedTopThreeTable() {
        // given
        saveCatalog()
        val expected =
            mapOf(
                "컨셉만" to
                    listOf(
                        "걸크러시 성수 스튜디오 패키지 | 선호 컨셉 '걸크러시' 태그",
                        "GIRL_CRUSH 홍대 댄스 패키지 | 선호 컨셉 '걸크러시' 태그",
                        "에스파 굿즈 투어 패키지 | 선호 컨셉 '걸크러시' 태그",
                    ),
                "장소만" to
                    listOf(
                        "걸크러시 성수 스튜디오 패키지 | 방문 장소 '성수', '홍대'",
                        "GIRL_CRUSH 홍대 댄스 패키지 | 방문 장소 '홍대'",
                        "러블리 청순 카페 투어 | 방문 장소 '성수'",
                    ),
                "기간만" to listOf(POPULAR_MARKER),
                "전부" to
                    listOf(
                        "걸크러시 성수 스튜디오 패키지 | 선호 컨셉 '걸크러시' 태그 | " +
                            "방문 장소 '성수', '홍대' | 방문 기간 4일 중 4일 예약 가능",
                        "시크 무드 화보 패키지 | 선호 컨셉 '걸크러시' 태그 | 방문 기간 4일 중 4일 예약 가능",
                        "GIRL_CRUSH 홍대 댄스 패키지 | 선호 컨셉 '걸크러시' 태그 | " +
                            "방문 장소 '홍대' | 방문 기간 4일 중 2일 예약 가능",
                    ),
            )

        // when
        val actual = surveyVariants().mapValues { (_, signals) -> summarize(recommend(signals)) }

        // then
        assertEquals(expected, actual)
    }

    @Test
    @DisplayName("컨셉만 설문은 태그가 정확히 같은 상품만 후보가 되어 '걸크러시/시크' 태그 상품은 빠진다")
    fun conceptOnlyMissesCompoundTagProduct() {
        // given
        saveCatalog()

        // when
        val result = recommend(surveyVariants().getValue("컨셉만"))

        // then
        assertEquals(
            listOf("걸크러시 성수 스튜디오 패키지", "GIRL_CRUSH 홍대 댄스 패키지", "에스파 굿즈 투어 패키지"),
            result.map { it.name },
        )
        assertTrue(result.none { it.name == "시크 무드 화보 패키지" })
    }

    @Test
    @DisplayName("기간만 설문은 일정 신호가 있어도 인기 상품(POPULAR)으로 대체되고 추천 이유가 없다")
    fun visitPeriodOnlyFallsBackToPopular() {
        // given
        saveCatalog()

        // when
        val result = recommend(surveyVariants().getValue("기간만"))

        // then
        assertTrue(result.isNotEmpty())
        assertTrue(result.all { it.fallback == RecommendationFallback.POPULAR && it.reasons.isEmpty() })
    }

    @Test
    @DisplayName("전부 설문에서 상품명에만 '성수'가 있는 상품은 일정 후보로 들어와야 장소 점수를 받는다")
    fun placeInProductNameScoresOnlyWhenCandidateBySchedule() {
        // given
        saveCatalog()
        val variants = surveyVariants()

        // when
        val placeOnly = recommend(variants.getValue("장소만"), size = TOTAL_PRODUCTS)
        val everything = recommend(variants.getValue("전부"), size = TOTAL_PRODUCTS)

        // then
        assertTrue(placeOnly.none { it.name == NAME_ONLY_PLACE_PRODUCT })
        val fourth = everything[TOP_SIZE]
        assertEquals(NAME_ONLY_PLACE_PRODUCT, fourth.name)
        assertEquals(
            listOf("방문 장소 '성수'", "방문 기간 4일 중 3일 예약 가능"),
            fourth.reasons.map { it.label },
        )
    }

    private fun recommend(
        signals: RecommendationSignals,
        size: Int = TOP_SIZE,
    ): List<ProductRankingResponse> = productDiscoveryService.findRecommendedProducts(signals, size)

    private fun summarize(result: List<ProductRankingResponse>): List<String> {
        if (result.all { it.fallback == RecommendationFallback.POPULAR && it.reasons.isEmpty() }) {
            return listOf(POPULAR_MARKER)
        }
        return result.take(TOP_SIZE).map { response ->
            (listOf(response.name) + response.reasons.map { it.label }).joinToString(" | ")
        }
    }

    private fun surveyVariants(): Map<String, RecommendationSignals> {
        val concept = setOf(ConceptType.GIRL_CRUSH.name)
        val places = listOf("성수", "홍대")
        return linkedMapOf(
            "컨셉만" to RecommendationSignals(conceptTags = concept),
            "장소만" to RecommendationSignals(placeKeywords = places),
            "기간만" to RecommendationSignals(visitStart = VISIT_START, visitEnd = VISIT_END),
            "전부" to
                RecommendationSignals(
                    conceptTags = concept,
                    idolTokens = listOf("에스파"),
                    placeKeywords = places,
                    visitStart = VISIT_START,
                    visitEnd = VISIT_END,
                ),
        )
    }

    private fun saveCatalog() {
        saveConceptAndPlaceProducts()
        saveScheduleProducts()
        entityManager.flush()
        entityManager.clear()
    }

    private fun saveConceptAndPlaceProducts() {
        val hongdaeShop = saveOption("메이크업 옵션", "홍대 메이크업샵")
        val seongsuPhotoZone = saveOption("야경 촬영 옵션", "서울 성동구 성수동 포토존")
        product(
            name = "걸크러시 성수 스튜디오 패키지",
            tags = listOf("걸크러시", "메이크업"),
            location = location("성수 스튜디오", "서울 성동구 성수이로 1"),
            options = listOf(hongdaeShop),
            openDays = 4,
        )
        product(
            name = "GIRL_CRUSH 홍대 댄스 패키지",
            tags = listOf("GIRL_CRUSH"),
            location = location("홍대 댄스 스튜디오", "서울 마포구 와우산로 1"),
            openDays = 2,
            bookedDays = 2,
        )
        product(
            name = "시크 무드 화보 패키지",
            tags = listOf("걸크러시/시크"),
            location = location("강남 화보 스튜디오", "서울 강남구 테헤란로 1"),
            openDays = 4,
        )
        product(
            name = "러블리 청순 카페 투어",
            tags = listOf("러블리", "청순"),
            location = location("성수 카페거리", "서울 성동구 연무장길 1"),
            openDays = 1,
            heldDays = 3,
        )
        product(
            name = "몽환 한강 야경 패키지",
            tags = listOf("DREAMY"),
            location = location("여의도 한강공원", "서울 영등포구 여의동로 1"),
            options = listOf(seongsuPhotoZone),
        )
    }

    private fun saveScheduleProducts() {
        product(
            name = "하이틴 교복 체험",
            tags = listOf("하이틴"),
            location = location("잠실 교복 대여점", "서울 송파구 올림픽로 1"),
        )
        product(
            name = "우아 글램 메이크업",
            tags = listOf("우아", "글램"),
            location = location("압구정 뷰티샵", "서울 강남구 압구정로 1"),
            bookedDays = 4,
        )
        product(
            name = "에스파 굿즈 투어 패키지",
            tags = listOf("걸크러시"),
            location = location("명동 굿즈샵", "서울 중구 명동길 1"),
            heldDays = 4,
        )
        product(
            name = "K-팝 일반 투어",
            tags = listOf("투어"),
            location = location("종로 투어센터", "서울 종로구 종로 1"),
            openDays = 1,
            bookedDays = 3,
        )
        product(
            name = NAME_ONLY_PLACE_PRODUCT,
            tags = listOf("뷰티"),
            location = location("강남 뷰티랩", "서울 강남구 논현로 1"),
            openDays = 3,
            bookedDays = 1,
        )
    }

    private fun saveOption(
        name: String,
        location: String,
    ): Option {
        val option = Option(name = name, description = "옵션", price = BigDecimal.ZERO, location = location)
        entityManager.persist(option)
        return option
    }

    private fun location(
        name: String,
        roadAddress: String,
    ): (Product) -> ProductLocation =
        { product ->
            ProductLocation(
                product = product,
                name = name,
                latitude = BigDecimal("37.5"),
                longitude = BigDecimal("127.0"),
                roadAddressName = roadAddress,
                addressName = null,
                kakaoPlaceId = "kakao-$name",
            )
        }

    private fun product(
        name: String,
        tags: List<String>,
        location: (Product) -> ProductLocation,
        options: List<Option> = emptyList(),
        openDays: Int = 0,
        bookedDays: Int = 0,
        heldDays: Int = 0,
    ) {
        val product = Product(name = name, description = "평가용 상품", basePrice = BigDecimal.TEN)
        product.addTags(tags)
        product.addOptions(options)
        product.setLocation(location(product))
        val totalDays = openDays + bookedDays + heldDays
        if (totalDays > 0) {
            product.addReservationSlots(VISIT_START, VISIT_START.plusDays(totalDays - 1L), SLOT_START, SLOT_END)
        }
        val slots = product.reservationSlots.sortedBy { it.reservationDate }
        slots.drop(openDays).take(bookedDays).forEach { slot ->
            slot.hold(BOOKING_ID, FIXED_NOW.plusHours(1), FIXED_NOW)
            slot.confirmBooking(BOOKING_ID, FIXED_NOW)
        }
        slots.drop(openDays + bookedDays).forEach { slot -> slot.hold(HOLD_ID, HOLD_UNTIL, FIXED_NOW) }
        entityManager.persist(product)
    }

    companion object {
        private const val TOP_SIZE = 3
        private const val TOTAL_PRODUCTS = 10
        private const val BOOKING_ID = 1L
        private const val HOLD_ID = 2L
        private const val POPULAR_MARKER = "POPULAR"
        private const val NAME_ONLY_PLACE_PRODUCT = "성수 뷰티 클래스"
        private val VISIT_START: LocalDate = LocalDate.of(2099, 5, 1)
        private val VISIT_END: LocalDate = LocalDate.of(2099, 5, 4)
        private val SLOT_START: LocalTime = LocalTime.of(10, 0)
        private val SLOT_END: LocalTime = LocalTime.of(11, 0)
        private val FIXED_NOW: LocalDateTime = LocalDateTime.of(2099, 1, 1, 0, 0)
        private val HOLD_UNTIL: LocalDateTime = LocalDateTime.of(2099, 12, 31, 0, 0)
    }
}
