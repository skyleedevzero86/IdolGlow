package com.sleekydz86.idolglow.idolglowissue.application

import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetPageResult
import com.sleekydz86.idolglow.idolglowissue.application.dto.PublicRankedArticleResponse
import com.sleekydz86.idolglow.idolglowissue.domain.IdolGlowArticleSort
import com.sleekydz86.idolglow.like.application.port.`in`.ContentLikeQueryUseCase
import com.sleekydz86.idolglow.like.domain.LikeTarget
import com.sleekydz86.idolglow.like.domain.LikeTargetType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class IdolGlowArticleRankingService(
    private val idolglowArticleRankingPort: IdolGlowArticleRankingPort,
    private val contentLikeQueryUseCase: ContentLikeQueryUseCase,
) : IdolGlowArticleRankingUseCase {
    internal var clock: Clock = Clock.system(IdolGlowDateTimes.ZONE)

    override fun findArticles(
        sort: IdolGlowArticleSort,
        pageRequest: OffsetPageRequest,
        viewerId: Long?,
    ): OffsetPageResult<PublicRankedArticleResponse> {
        val slice = idolglowArticleRankingPort.findPublished(sort, pageRequest, LocalDate.now(clock))
        val likedKeys =
            if (viewerId == null || slice.items.isEmpty()) {
                emptySet()
            } else {
                contentLikeQueryUseCase
                    .summarize(slice.items.map { LikeTarget(LikeTargetType.ARTICLE, it.contentKey) }, viewerId)
                    .filterValues { it.liked }
                    .keys
                    .map { it.key }
                    .toSet()
            }
        return OffsetPageResult.of(
            items = slice.items.map { PublicRankedArticleResponse.of(it, liked = it.contentKey in likedKeys) },
            request = pageRequest,
            totalElements = slice.totalElements,
        )
    }
}
