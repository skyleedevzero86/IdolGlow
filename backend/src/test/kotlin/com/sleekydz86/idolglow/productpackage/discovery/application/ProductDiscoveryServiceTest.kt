package com.sleekydz86.idolglow.productpackage.discovery.application

import com.sleekydz86.idolglow.image.application.AggregateImageQueryService
import com.sleekydz86.idolglow.productpackage.discovery.infrastructure.ProductDiscoveryQueryRepository
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.user.user.domain.UserSurveyRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("개인화 추천 상품 조회")
class ProductDiscoveryServiceTest {
    private val repository = mockk<ProductDiscoveryQueryRepository>()
    private val userSurveys = mockk<UserSurveyRepository>()
    private val images = mockk<AggregateImageQueryService>()
    private val service = ProductDiscoveryService(repository, userSurveys, images)
    private val popular = Product(id = 9L, name = "인기 상품", description = "인기")

    @BeforeEach
    fun setUp() {
        every { images.firstProductImageUrlByProductIds(any()) } returns emptyMap()
        every { repository.findPopularProductIds(any()) } returns listOf(popular.id)
        every { repository.findProductsByIds(listOf(popular.id)) } returns listOf(popular)
        every { repository.findTagNamesByProductIds(any()) } returns emptyMap()
        every { repository.findWishCounts(any()) } returns emptyMap()
        every { repository.findReviewMetrics(any()) } returns emptyMap()
    }

    @Test
    @DisplayName("신호가 비어 있으면 인기 상품을 POPULAR 표시와 함께 돌려준다")
    fun emptySignalsFallBackToPopular() {
        // given
        every { userSurveys.findByUserId(1L) } returns null
        every { repository.findPreferredTags(1L, any()) } returns emptyList()

        // when
        val result = service.findRecommendedProducts(1L, 5)

        // then
        assertEquals(listOf(popular.id), result.map { it.id })
        assertTrue(result.all { it.fallback == RecommendationFallback.POPULAR && it.reasons.isEmpty() })
        verify(exactly = 0) { repository.findPersonalizedCandidateProductIds(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    @DisplayName("후보가 0건이면 인기 상품을 POPULAR 표시와 함께 돌려준다")
    fun noCandidateFallsBackToPopular() {
        // given
        val signals = RecommendationSignals(extraTags = setOf("없는태그"))
        every {
            repository.findPersonalizedCandidateProductIds(any(), any(), any(), any(), any(), any())
        } returns emptyList()
        every { repository.findRecommendedProductIds(any(), any()) } returns emptyList()

        // when
        val result = service.findRecommendedProducts(signals, 5)

        // then
        assertEquals(listOf(popular.id), result.map { it.id })
        assertEquals(RecommendationFallback.POPULAR, result.single().fallback)
    }

    @Test
    @DisplayName("후보 조회 태그에 컨셉 별칭과 폼 태그를 모두 넣는다")
    fun candidateTagsIncludeConceptAliasesAndExtraTags() {
        // given
        val signals = RecommendationSignals(conceptTags = setOf("LOVELY_FRESH"), extraTags = setOf("포토"))
        every {
            repository.findPersonalizedCandidateProductIds(any(), any(), any(), any(), any(), any())
        } returns emptyList()
        every { repository.findRecommendedProductIds(any(), any()) } returns emptyList()

        // when
        service.findRecommendedProducts(signals, 5)

        // then
        verify {
            repository.findPersonalizedCandidateProductIds(
                match { it.containsAll(listOf("LOVELY_FRESH", "러블리, 청순", "러블리", "청순", "포토")) },
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
    }
}
