package com.sleekydz86.idolglow.webzine.application

import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetSlice
import com.sleekydz86.idolglow.webzine.application.dto.RankedWebzineArticle
import com.sleekydz86.idolglow.webzine.domain.WebzineArticleSort
import java.time.LocalDate

interface WebzineArticleRankingPort {
    fun findPublished(
        sort: WebzineArticleSort,
        request: OffsetPageRequest,
        today: LocalDate,
    ): OffsetSlice<RankedWebzineArticle>
}
