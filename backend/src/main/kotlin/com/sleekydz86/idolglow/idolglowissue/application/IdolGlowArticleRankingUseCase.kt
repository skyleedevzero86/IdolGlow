package com.sleekydz86.idolglow.idolglowissue.application

import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetPageResult
import com.sleekydz86.idolglow.idolglowissue.application.dto.PublicRankedArticleResponse
import com.sleekydz86.idolglow.idolglowissue.domain.IdolGlowArticleSort

interface IdolGlowArticleRankingUseCase {
    fun findArticles(
        sort: IdolGlowArticleSort,
        pageRequest: OffsetPageRequest,
        viewerId: Long?,
    ): OffsetPageResult<PublicRankedArticleResponse>
}
