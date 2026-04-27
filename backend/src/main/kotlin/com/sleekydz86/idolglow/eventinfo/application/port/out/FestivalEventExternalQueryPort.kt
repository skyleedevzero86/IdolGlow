package com.sleekydz86.idolglow.eventinfo.application.port.out

import com.sleekydz86.idolglow.eventinfo.domain.CodeEntry
import com.sleekydz86.idolglow.eventinfo.domain.FestivalCommonDetail
import com.sleekydz86.idolglow.eventinfo.domain.FestivalEvent
import com.sleekydz86.idolglow.eventinfo.domain.FestivalImage

interface FestivalEventExternalQueryPort {
    fun searchFestival(
        eventStartDate: String,
        eventEndDate: String,
        pageNo: Int,
        numOfRows: Int,
    ): List<FestivalEvent>

    fun detailCommon(contentId: String): FestivalCommonDetail?

    fun detailImage(contentId: String, imageYn: String = "Y"): List<FestivalImage>

    fun searchKeyword(
        keyword: String,
        pageNo: Int,
        numOfRows: Int,
        lDongRegnCd: String? = null,
        lDongSignguCd: String? = null,
        lclsSystm1: String? = null,
        lclsSystm2: String? = null,
        lclsSystm3: String? = null,
    ): List<FestivalEvent>

    fun lDongCodes(lDongRegnCd: String?, lDongListYn: String): List<CodeEntry>

    fun lclsCodes(
        lclsSystm1: String?,
        lclsSystm2: String?,
        lclsSystm3: String?,
        lclsSystmListYn: String,
    ): List<CodeEntry>
}
