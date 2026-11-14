package com.sleekydz86.idolglow.webzine.application

import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetPageResult
import com.sleekydz86.idolglow.webzine.application.dto.PublicRankedArticleResponse
import com.sleekydz86.idolglow.webzine.domain.WebzineArticleSort

interface WebzineArticleRankingUseCase {
    fun findArticles(
        sort: WebzineArticleSort,
        pageRequest: OffsetPageRequest,
        viewerId: Long?,
    ): OffsetPageResult<PublicRankedArticleResponse>
}
