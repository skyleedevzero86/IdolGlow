package com.sleekydz86.idolglow.survey.application

import com.sleekydz86.idolglow.image.application.AggregateImageQueryService
import com.sleekydz86.idolglow.productpackage.attraction.application.port.out.TourAttractionQueryPort
import com.sleekydz86.idolglow.productpackage.discovery.application.ProductDiscoveryService
import com.sleekydz86.idolglow.productpackage.discovery.application.RecommendationReasonCode
import com.sleekydz86.idolglow.productpackage.discovery.infrastructure.ProductDiscoveryQueryRepository
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.survey.domain.RecommendationSignalType
import com.sleekydz86.idolglow.survey.domain.SurveySubmission
import com.sleekydz86.idolglow.survey.domain.dto.SurveyUsedSignalSource
import com.sleekydz86.idolglow.survey.domain.dto.SurveyUsedSignalType
import com.sleekydz86.idolglow.survey.infrastructure.SurveyLlmRecommendationStore
import com.sleekydz86.idolglow.survey.infrastructure.SurveySubmissionJpaRepository
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.UserSurvey
import com.sleekydz86.idolglow.user.user.domain.UserSurveyRepository
import com.sleekydz86.idolglow.user.user.domain.vo.ConceptType
import com.sleekydz86.idolglow.user.user.domain.vo.Nickname
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("설문지 폼 추천")
class SurveyFormRecommendationTest {
    private val submissions = mockk<SurveySubmissionJpaRepository>()
    private val discoveryRepository = mockk<ProductDiscoveryQueryRepository>()
    private val userSurveys = mockk<UserSurveyRepository>()
    private val images = mockk<AggregateImageQueryService>()
    private val tourAttractions = mockk<TourAttractionQueryPort>()
    private val store = mockk<SurveyLlmRecommendationStore>(relaxed = true)
    private val discovery = ProductDiscoveryService(discoveryRepository, userSurveys, images)
    private val service = SurveyRecommendationService(submissions, discovery, tourAttractions, store)

    private val photo = Product(id = 1L, name = "스튜디오 촬영", description = "프로필 사진")
    private val hair = Product(id = 2L, name = "헤어 스타일링", description = "무대 헤어")

    @BeforeEach
    fun setUp() {
        every { userSurveys.findByUserId(USER_ID) } returns null
        every { discoveryRepository.findPreferredTags(USER_ID, any()) } returns emptyList()
        every {
            discoveryRepository.findPersonalizedCandidateProductIds(any(), any(), any(), any(), any(), any())
        } returns listOf(photo.id, hair.id)
        every { discoveryRepository.findProductsByIds(any()) } returns listOf(photo, hair)
        every { discoveryRepository.findTagNamesByProductIds(any()) } returns
            mapOf(photo.id to listOf("포토"), hair.id to listOf("헤어"))
        every { discoveryRepository.findWishCounts(any()) } returns mapOf(hair.id to 3L)
        every { discoveryRepository.findReviewMetrics(any()) } returns emptyMap()
        every { discoveryRepository.countAvailableTripDaysByProduct(any(), any(), any(), any()) } returns emptyMap()
        every { images.firstProductImageUrlByProductIds(any()) } returns emptyMap()
        every { tourAttractions.fetchAreaBasedAttractions(any(), any(), any(), any()) } returns emptyList()
    }

    private fun submit(submission: SurveySubmission) {
        every { submissions.findByIdAndUserId(submission.id, USER_ID) } returns submission
    }

    @Test
    @DisplayName("PLACE 응답 '성동구'면 성동구 코드로 관광지를 조회한다")
    fun formPlaceSignalSelectsDistrict() {
        // given
        val submission = SurveyFormFixtures().text(RecommendationSignalType.PLACE, "성동구").build()
        submit(submission)

        // when
        service.generate(USER_ID, submission.id, useLlm = false)

        // then
        verify { tourAttractions.fetchAreaBasedAttractions(any(), 11, 11200, any()) }
    }

    @Test
    @DisplayName("구를 찾지 못하면 구 조건 없이 서울 전체로 조회한다")
    fun unknownPlaceUsesWholeSeoul() {
        // given
        val submission = SurveyFormFixtures().text(RecommendationSignalType.PLACE, "성수 카페거리").build()
        submit(submission)

        // when
        service.generate(USER_ID, submission.id, useLlm = false)

        // then
        verify { tourAttractions.fetchAreaBasedAttractions(any(), 11, null, any()) }
        verify(exactly = 0) { tourAttractions.fetchAreaBasedAttractions(any(), any(), 11530, any()) }
    }

    @Test
    @DisplayName("폼 TAG 신호와 맞는 상품이 회원 추천과 같은 점수로 위에 오고 EXTRA_TAG 이유를 받는다")
    fun formRecommendationUsesSharedScorer() {
        // given
        val submission = SurveyFormFixtures().choice(RecommendationSignalType.TAG, "포토").build()
        submit(submission)

        // when
        val response = service.generate(USER_ID, submission.id, useLlm = false)

        // then
        assertEquals(listOf(photo.id, hair.id), response.recommendedProducts.map { it.id })
        val top = response.recommendedProducts.first()
        assertEquals(listOf(RecommendationReasonCode.EXTRA_TAG), top.reasons.map { it.code })
        assertEquals(null, top.fallback)
        val used = response.usedSignals.single()
        assertEquals(SurveyUsedSignalType.TAG, used.type)
        assertEquals(SurveyUsedSignalSource.FORM, used.source)
        assertEquals(listOf("포토"), used.values)
        verify(exactly = 0) { discoveryRepository.findPopularProductIds(any()) }
    }

    @Test
    @DisplayName("폼 추천은 회원 설문을 읽기만 하고 바꾸지 않는다")
    fun formRecommendationDoesNotModifyUserSurvey() {
        // given
        val user = User(id = USER_ID, email = "fan@example.com", nickname = Nickname.of("팬하나"))
        val userSurvey =
            UserSurvey.of(
                user = user,
                concept = ConceptType.DREAMY,
                idolName = "아이유",
                visitStartDate = LocalDate.of(2026, 12, 1),
                visitEndDate = LocalDate.of(2026, 12, 4),
                visitStartTime = null,
                visitEndTime = null,
                places = listOf("홍대"),
            )
        every { userSurveys.findByUserId(USER_ID) } returns userSurvey
        val submission =
            SurveyFormFixtures()
                .choice(RecommendationSignalType.CONCEPT, "걸크러시")
                .text(RecommendationSignalType.PLACE, "성동구")
                .build()
        submit(submission)

        // when
        val response = service.generate(USER_ID, submission.id, useLlm = false)

        // then
        verify(exactly = 0) { userSurveys.save(any()) }
        verify(exactly = 0) { userSurveys.deleteByUserId(any()) }
        assertEquals(ConceptType.DREAMY, userSurvey.concept)
        assertEquals(listOf("홍대"), userSurvey.places)
        assertEquals(LocalDate.of(2026, 12, 1), userSurvey.visitStartDate)
        val period = response.usedSignals.single { it.type == SurveyUsedSignalType.VISIT_PERIOD }
        assertEquals(SurveyUsedSignalSource.USER_SURVEY, period.source)
        verify { tourAttractions.fetchAreaBasedAttractions(any(), 11, 11200, any()) }
    }

    companion object {
        private const val USER_ID = 1L
    }
}
