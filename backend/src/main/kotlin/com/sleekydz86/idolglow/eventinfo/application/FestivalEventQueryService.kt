package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.incoming.FestivalEventQueryUseCase
import com.sleekydz86.idolglow.eventinfo.application.port.out.FestivalEventExternalQueryPort
import com.sleekydz86.idolglow.eventinfo.domain.CodeEntry
import com.sleekydz86.idolglow.eventinfo.domain.FestivalCommonDetail
import com.sleekydz86.idolglow.eventinfo.domain.FestivalEvent
import com.sleekydz86.idolglow.eventinfo.domain.FestivalImage
import org.springframework.stereotype.Service

@Service
class FestivalEventQueryService(
    private val festivalEventExternalQueryPort: FestivalEventExternalQueryPort,
) : FestivalEventQueryUseCase {

    override fun listByDate(
        eventStartDate: String,
        eventEndDate: String,
        pageNo: Int,
        numOfRows: Int,
    ): List<FestivalEvent> {
        val start = eventStartDate.trim()
        val end = eventEndDate.trim()
        require(YYYYMMDD.matches(start)) { "eventStartDate는 YYYYMMDD 형식이어야 합니다." }
        require(YYYYMMDD.matches(end)) { "eventEndDate는 YYYYMMDD 형식이어야 합니다." }
        val page = pageNo.coerceAtLeast(1)
        val rows = numOfRows.coerceIn(1, 100)
        return festivalEventExternalQueryPort.searchFestival(start, end, page, rows)
    }

    override fun detailCommon(contentId: String): FestivalCommonDetail? {
        val id = contentId.trim()
        require(id.isNotEmpty()) { "contentId는 필수입니다." }
        return festivalEventExternalQueryPort.detailCommon(id)
    }

    override fun detailImages(contentId: String, imageYn: String): List<FestivalImage> {
        val id = contentId.trim()
        val yn = imageYn.trim().uppercase().ifEmpty { "Y" }
        require(id.isNotEmpty()) { "contentId는 필수입니다." }
        require(yn == "Y" || yn == "N") { "imageYn은 Y 또는 N 이어야 합니다." }
        return festivalEventExternalQueryPort.detailImage(id, yn)
    }

    override fun searchKeyword(
        keyword: String,
        pageNo: Int,
        numOfRows: Int,
        lDongRegnCd: String?,
        lDongSignguCd: String?,
        lclsSystm1: String?,
        lclsSystm2: String?,
        lclsSystm3: String?,
    ): List<FestivalEvent> {
        val trimmed = keyword.trim()
        val regn = lDongRegnCd?.trim()?.takeIf { it.isNotEmpty() }
        val signgu = lDongSignguCd?.trim()?.takeIf { it.isNotEmpty() }
        val c1 = lclsSystm1?.trim()?.takeIf { it.isNotEmpty() }
        val c2 = lclsSystm2?.trim()?.takeIf { it.isNotEmpty() }
        val c3 = lclsSystm3?.trim()?.takeIf { it.isNotEmpty() }
        val hasFilter = regn != null || signgu != null || c1 != null || c2 != null || c3 != null
        val q = when {
            trimmed.isNotEmpty() -> trimmed
            hasFilter -> DEFAULT_KEYWORD_FOR_FILTER_ONLY_SEARCH
            else -> throw IllegalArgumentException("keyword는 필수입니다. 지역·분류만 검색할 때는 조건을 하나 이상 선택하세요.")
        }
        val page = pageNo.coerceAtLeast(1)
        val rows = numOfRows.coerceIn(1, 100)
        if (trimmed.isEmpty()) {
            return festivalEventExternalQueryPort.areaBasedList(
                pageNo = page,
                numOfRows = rows,
                lDongRegnCd = regn,
                lDongSignguCd = signgu,
                lclsSystm1 = c1,
                lclsSystm2 = c2,
                lclsSystm3 = c3,
            )
        }
        return festivalEventExternalQueryPort.searchKeyword(
            keyword = q,
            pageNo = page,
            numOfRows = rows,
            lDongRegnCd = regn,
            lDongSignguCd = signgu,
            lclsSystm1 = c1,
            lclsSystm2 = c2,
            lclsSystm3 = c3,
        )
    }

    override fun lDongCodes(lDongRegnCd: String?, lDongListYn: String): List<CodeEntry> {
        val yn = lDongListYn.trim().uppercase().ifEmpty { "N" }
        require(yn == "Y" || yn == "N") { "lDongListYn은 Y 또는 N 이어야 합니다." }
        return festivalEventExternalQueryPort.lDongCodes(lDongRegnCd?.trim()?.takeIf { it.isNotEmpty() }, yn)
    }

    override fun lclsCodes(
        lclsSystm1: String?,
        lclsSystm2: String?,
        lclsSystm3: String?,
        lclsSystmListYn: String,
    ): List<CodeEntry> {
        val yn = lclsSystmListYn.trim().uppercase().ifEmpty { "N" }
        require(yn == "Y" || yn == "N") { "lclsSystmListYn은 Y 또는 N 이어야 합니다." }
        return festivalEventExternalQueryPort.lclsCodes(
            lclsSystm1?.trim()?.takeIf { it.isNotEmpty() },
            lclsSystm2?.trim()?.takeIf { it.isNotEmpty() },
            lclsSystm3?.trim()?.takeIf { it.isNotEmpty() },
            yn,
        )
    }

    companion object {
        private val YYYYMMDD = Regex("^\\d{8}$")
        private const val DEFAULT_KEYWORD_FOR_FILTER_ONLY_SEARCH = "축제"
    }
}
