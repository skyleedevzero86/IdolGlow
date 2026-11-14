package com.sleekydz86.idolglow.idolglowissue.application

import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetSlice
import com.sleekydz86.idolglow.idolglowissue.application.dto.RankedIdolGlowArticle
import com.sleekydz86.idolglow.idolglowissue.domain.IdolGlowArticleSort
import java.time.LocalDate

interface IdolGlowArticleRankingPort {
    fun findPublished(
        sort: IdolGlowArticleSort,
        request: OffsetPageRequest,
        today: LocalDate,
    ): OffsetSlice<RankedIdolGlowArticle>
}
