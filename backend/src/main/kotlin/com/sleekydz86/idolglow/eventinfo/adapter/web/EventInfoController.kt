package com.sleekydz86.idolglow.eventinfo.adapter.web

import com.sleekydz86.idolglow.eventinfo.adapter.web.dto.CodeEntryResponse
import com.sleekydz86.idolglow.eventinfo.adapter.web.dto.FestivalEventResponse
import com.sleekydz86.idolglow.eventinfo.adapter.web.dto.FestivalCommonDetailResponse
import com.sleekydz86.idolglow.eventinfo.adapter.web.dto.FestivalImageResponse
import com.sleekydz86.idolglow.eventinfo.adapter.web.dto.KopisAreaStatResponse
import com.sleekydz86.idolglow.eventinfo.adapter.web.dto.SpecialDayInfoResponse
import com.sleekydz86.idolglow.eventinfo.application.port.incoming.FestivalEventQueryUseCase
import com.sleekydz86.idolglow.eventinfo.infrastructure.KopisPerformanceApiClient
import com.sleekydz86.idolglow.eventinfo.infrastructure.SeoulSjwPerformApiClient
import com.sleekydz86.idolglow.eventinfo.infrastructure.SpcdeInfoApiClient
import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Tag(name = "Glow 행사 정보")
@RestController
@RequestMapping("/mypage/event-info", "/api/event-info")
class EventInfoController(
    private val festivalEventQueryUseCase: FestivalEventQueryUseCase,
    private val seoulSjwPerformApiClient: SeoulSjwPerformApiClient,
    private val kopisPerformanceApiClient: KopisPerformanceApiClient,
    private val spcdeInfoApiClient: SpcdeInfoApiClient,
) {

    @GetMapping("/festivals")
    fun festivals(
        @LoginUser userId: Long,
        @RequestParam(required = false) date: String?,
        @RequestParam(required = false, defaultValue = "1") pageNo: Int,
        @RequestParam(required = false, defaultValue = "20") numOfRows: Int,
    ): List<FestivalEventResponse> {
        check(userId > 0L) { "로그인이 필요합니다." }
        val yyyyMMdd = normalizeDate(date)
        val tourEvents = festivalEventQueryUseCase.listByDate(
            eventStartDate = yyyyMMdd,
            eventEndDate = yyyyMMdd,
            pageNo = pageNo,
            numOfRows = numOfRows,
        )
        val sjwEvents = seoulSjwPerformApiClient
            .list(pageNo = pageNo, numOfRows = numOfRows)
            .filter { seoulSjwPerformApiClient.isEventActiveOnDate(it, yyyyMMdd) }
        val kopisEvents = kopisPerformanceApiClient.listPerformances(
            stDate = yyyyMMdd,
            edDate = yyyyMMdd,
            page = pageNo,
            rows = numOfRows,
            signguCode = "11",
            prfState = "02",
        )
        return (tourEvents + sjwEvents + kopisEvents).map { event ->
            FestivalEventResponse(
                contentId = event.contentId,
                title = event.title,
                address = event.address,
                eventStartDate = event.eventStartDate,
                eventEndDate = event.eventEndDate,
                thumbnailImageUrl = event.thumbnailImageUrl,
                imageUrl = event.imageUrl,
                mapX = event.mapX,
                mapY = event.mapY,
                phone = event.phone,
                detailUrl = event.detailUrl,
                category = event.category,
                synopsis = event.synopsis,
                source = event.source,
                cast = event.cast,
                runningTime = event.runningTime,
                age = event.age,
                bookingPlaces = event.bookingPlaces,
                introImageUrls = event.introImageUrls,
            )
        }
    }

    @GetMapping("/kopis/performances")
    fun kopisPerformances(
        @LoginUser userId: Long,
        @RequestParam(required = false) date: String?,
        @RequestParam(required = false, defaultValue = "1") pageNo: Int,
        @RequestParam(required = false, defaultValue = "20") numOfRows: Int,
        @RequestParam(required = false, defaultValue = "11") signgucode: String,
        @RequestParam(required = false, defaultValue = "02") prfstate: String,
    ): List<FestivalEventResponse> {
        check(userId > 0L) { "로그인이 필요합니다." }
        val yyyymmdd = normalizeDate(date)
        return kopisPerformanceApiClient.listPerformances(
            stDate = yyyymmdd,
            edDate = yyyymmdd,
            page = pageNo,
            rows = numOfRows,
            signguCode = signgucode,
            prfState = prfstate,
        ).map(::toFestivalResponse)
    }

    @GetMapping("/kopis/performances/{mt20id}")
    fun kopisPerformanceDetail(
        @LoginUser userId: Long,
        @PathVariable mt20id: String,
    ): FestivalEventResponse? {
        check(userId > 0L) { "로그인이 필요합니다." }
        return kopisPerformanceApiClient.detailPerformance(mt20id)?.let(::toFestivalResponse)
    }

    @GetMapping("/kopis/festivals")
    fun kopisFestivals(
        @LoginUser userId: Long,
        @RequestParam(required = false) date: String?,
        @RequestParam(required = false, defaultValue = "1") pageNo: Int,
        @RequestParam(required = false, defaultValue = "20") numOfRows: Int,
    ): List<FestivalEventResponse> {
        check(userId > 0L) { "로그인이 필요합니다." }
        val yyyymmdd = normalizeDate(date)
        return kopisPerformanceApiClient.listFestivals(
            stDate = yyyymmdd,
            edDate = yyyymmdd,
            page = pageNo,
            rows = numOfRows,
        ).map(::toFestivalResponse)
    }

    @GetMapping("/kopis/area-stats")
    fun kopisAreaStats(
        @LoginUser userId: Long,
        @RequestParam(required = false) stDate: String?,
        @RequestParam(required = false) edDate: String?,
    ): List<KopisAreaStatResponse> {
        check(userId > 0L) { "로그인이 필요합니다." }
        val start = normalizeDate(stDate)
        val end = normalizeDate(edDate ?: stDate)
        require(daysBetween(start, end) <= 31) { "KOPIS 지역통계는 최대 31일까지만 조회할 수 있습니다." }
        return kopisPerformanceApiClient.areaStats(start, end).map {
            KopisAreaStatResponse(
                area = it.area,
                fcltycnt = it.fcltycnt,
                prfplccnt = it.prfplccnt,
                seatcnt = it.seatcnt,
                prfcnt = it.prfcnt,
                prfprocnt = it.prfprocnt,
                prfdtcnt = it.prfdtcnt,
                nmrs = it.nmrs,
                nmrcancl = it.nmrcancl,
                totnmrs = it.totnmrs,
                amount = it.amount,
            )
        }
    }

    @GetMapping("/special-days/holidays")
    fun specialHolidays(
        @LoginUser userId: Long,
        @RequestParam(required = false) solYear: String?,
        @RequestParam(required = false) solMonth: String?,
    ): List<SpecialDayInfoResponse> {
        check(userId > 0L) { "로그인이 필요합니다." }
        val (year, month) = normalizeYearMonth(solYear, solMonth)
        return spcdeInfoApiClient.getHoliDeInfo(year, month).map(::toSpecialDayResponse)
    }

    @GetMapping("/special-days/rest-days")
    fun specialRestDays(
        @LoginUser userId: Long,
        @RequestParam(required = false) solYear: String?,
        @RequestParam(required = false) solMonth: String?,
    ): List<SpecialDayInfoResponse> {
        check(userId > 0L) { "로그인이 필요합니다." }
        val (year, month) = normalizeYearMonth(solYear, solMonth)
        return spcdeInfoApiClient.getRestDeInfo(year, month).map(::toSpecialDayResponse)
    }

    @GetMapping("/special-days/anniversaries")
    fun specialAnniversaries(
        @LoginUser userId: Long,
        @RequestParam(required = false) solYear: String?,
        @RequestParam(required = false) solMonth: String?,
    ): List<SpecialDayInfoResponse> {
        check(userId > 0L) { "로그인이 필요합니다." }
        val (year, month) = normalizeYearMonth(solYear, solMonth)
        return spcdeInfoApiClient.getAnniversaryInfo(year, month).map(::toSpecialDayResponse)
    }

    @GetMapping("/sjw-perform")
    fun sjwList(
        @LoginUser userId: Long,
        @RequestParam(required = false, defaultValue = "1") pageNo: Int,
        @RequestParam(required = false, defaultValue = "20") numOfRows: Int,
    ): List<FestivalEventResponse> {
        check(userId > 0L) { "로그인이 필요합니다." }
        return seoulSjwPerformApiClient.list(pageNo, numOfRows).map { event ->
            FestivalEventResponse(
                contentId = event.contentId,
                title = event.title,
                address = event.address,
                eventStartDate = event.eventStartDate,
                eventEndDate = event.eventEndDate,
                thumbnailImageUrl = event.thumbnailImageUrl,
                imageUrl = event.imageUrl,
                mapX = event.mapX,
                mapY = event.mapY,
                phone = event.phone,
                detailUrl = event.detailUrl,
                category = event.category,
                synopsis = event.synopsis,
                source = event.source,
                cast = event.cast,
                runningTime = event.runningTime,
                age = event.age,
                bookingPlaces = event.bookingPlaces,
                introImageUrls = event.introImageUrls,
            )
        }
    }

    @GetMapping("/sjw-perform/{performIdx}")
    fun sjwDetail(
        @LoginUser userId: Long,
        @PathVariable performIdx: String,
    ): FestivalEventResponse? {
        check(userId > 0L) { "로그인이 필요합니다." }
        val event = seoulSjwPerformApiClient.detail(performIdx) ?: return null
        return FestivalEventResponse(
            contentId = event.contentId,
            title = event.title,
            address = event.address,
            eventStartDate = event.eventStartDate,
            eventEndDate = event.eventEndDate,
            thumbnailImageUrl = event.thumbnailImageUrl,
            imageUrl = event.imageUrl,
            mapX = event.mapX,
            mapY = event.mapY,
            phone = event.phone,
            detailUrl = event.detailUrl,
            category = event.category,
            synopsis = event.synopsis,
            source = event.source,
            cast = event.cast,
            runningTime = event.runningTime,
            age = event.age,
            bookingPlaces = event.bookingPlaces,
            introImageUrls = event.introImageUrls,
        )
    }

    @GetMapping("/festivals/{contentId}/common")
    fun festivalCommon(
        @LoginUser userId: Long,
        @PathVariable contentId: String,
    ): FestivalCommonDetailResponse? {
        check(userId > 0L) { "로그인이 필요합니다." }
        return festivalEventQueryUseCase.detailCommon(contentId)?.let {
            FestivalCommonDetailResponse(
                contentId = it.contentId,
                contentTypeId = it.contentTypeId,
                title = it.title,
                homepage = it.homepage,
                overview = it.overview,
                address = it.address,
                addressDetail = it.addressDetail,
                mapX = it.mapX,
                mapY = it.mapY,
                tel = it.tel,
                firstImage = it.firstImage,
                firstImage2 = it.firstImage2,
            )
        }
    }

    @GetMapping("/festivals/{contentId}/images")
    fun festivalImages(
        @LoginUser userId: Long,
        @PathVariable contentId: String,
        @RequestParam(required = false, defaultValue = "Y") imageYn: String,
    ): List<FestivalImageResponse> {
        check(userId > 0L) { "로그인이 필요합니다." }
        return festivalEventQueryUseCase.detailImages(contentId, imageYn).map {
            FestivalImageResponse(
                contentId = it.contentId,
                imageName = it.imageName,
                originImageUrl = it.originImageUrl,
                smallImageUrl = it.smallImageUrl,
                copyrightType = it.copyrightType,
                serialNum = it.serialNum,
            )
        }
    }

    @GetMapping("/search-keyword")
    fun searchKeyword(
        @LoginUser userId: Long,
        @RequestParam keyword: String,
        @RequestParam(required = false, defaultValue = "1") pageNo: Int,
        @RequestParam(required = false, defaultValue = "20") numOfRows: Int,
        @RequestParam(required = false) lDongRegnCd: String?,
        @RequestParam(required = false) lDongSignguCd: String?,
        @RequestParam(required = false) lclsSystm1: String?,
        @RequestParam(required = false) lclsSystm2: String?,
        @RequestParam(required = false) lclsSystm3: String?,
    ): List<FestivalEventResponse> {
        check(userId > 0L) { "로그인이 필요합니다." }
        return festivalEventQueryUseCase.searchKeyword(
            keyword = keyword,
            pageNo = pageNo,
            numOfRows = numOfRows,
            lDongRegnCd = lDongRegnCd,
            lDongSignguCd = lDongSignguCd,
            lclsSystm1 = lclsSystm1,
            lclsSystm2 = lclsSystm2,
            lclsSystm3 = lclsSystm3,
        ).map { event ->
            FestivalEventResponse(
                contentId = event.contentId,
                title = event.title,
                address = event.address,
                eventStartDate = event.eventStartDate,
                eventEndDate = event.eventEndDate,
                thumbnailImageUrl = event.thumbnailImageUrl,
                imageUrl = event.imageUrl,
                mapX = event.mapX,
                mapY = event.mapY,
                phone = event.phone,
                detailUrl = event.detailUrl,
                category = event.category,
                synopsis = event.synopsis,
                source = event.source,
                cast = event.cast,
                runningTime = event.runningTime,
                age = event.age,
                bookingPlaces = event.bookingPlaces,
                introImageUrls = event.introImageUrls,
            )
        }
    }

    @GetMapping("/ldong-codes")
    fun lDongCodes(
        @LoginUser userId: Long,
        @RequestParam(required = false) lDongRegnCd: String?,
        @RequestParam(required = false, defaultValue = "N") lDongListYn: String,
    ): List<CodeEntryResponse> {
        check(userId > 0L) { "로그인이 필요합니다." }
        return festivalEventQueryUseCase.lDongCodes(lDongRegnCd, lDongListYn).map {
            CodeEntryResponse(
                code = it.code,
                name = it.name,
                rnum = it.rnum,
                lDongRegnCd = it.lDongRegnCd,
                lDongRegnNm = it.lDongRegnNm,
                lDongSignguCd = it.lDongSignguCd,
                lDongSignguNm = it.lDongSignguNm,
                lclsSystm1Cd = it.lclsSystm1Cd,
                lclsSystm1Nm = it.lclsSystm1Nm,
                lclsSystm2Cd = it.lclsSystm2Cd,
                lclsSystm2Nm = it.lclsSystm2Nm,
                lclsSystm3Cd = it.lclsSystm3Cd,
                lclsSystm3Nm = it.lclsSystm3Nm,
            )
        }
    }

    @GetMapping("/lcls-codes")
    fun lclsCodes(
        @LoginUser userId: Long,
        @RequestParam(required = false) lclsSystm1: String?,
        @RequestParam(required = false) lclsSystm2: String?,
        @RequestParam(required = false) lclsSystm3: String?,
        @RequestParam(required = false, defaultValue = "N") lclsSystmListYn: String,
    ): List<CodeEntryResponse> {
        check(userId > 0L) { "로그인이 필요합니다." }
        return festivalEventQueryUseCase.lclsCodes(
            lclsSystm1 = lclsSystm1,
            lclsSystm2 = lclsSystm2,
            lclsSystm3 = lclsSystm3,
            lclsSystmListYn = lclsSystmListYn,
        ).map {
            CodeEntryResponse(
                code = it.code,
                name = it.name,
                rnum = it.rnum,
                lDongRegnCd = it.lDongRegnCd,
                lDongRegnNm = it.lDongRegnNm,
                lDongSignguCd = it.lDongSignguCd,
                lDongSignguNm = it.lDongSignguNm,
                lclsSystm1Cd = it.lclsSystm1Cd,
                lclsSystm1Nm = it.lclsSystm1Nm,
                lclsSystm2Cd = it.lclsSystm2Cd,
                lclsSystm2Nm = it.lclsSystm2Nm,
                lclsSystm3Cd = it.lclsSystm3Cd,
                lclsSystm3Nm = it.lclsSystm3Nm,
            )
        }
    }

    private fun normalizeDate(input: String?): String {
        if (input.isNullOrBlank()) {
            return LocalDate.now().format(DATE_FMT)
        }
        val normalized = input.trim().replace("-", "")
        require(YYYYMMDD.matches(normalized)) { "date는 yyyy-MM-dd 또는 yyyyMMdd 형식이어야 합니다." }
        return normalized
    }

    private fun normalizeYearMonth(solYear: String?, solMonth: String?): Pair<String, String> {
        val year = solYear?.trim().orEmpty()
        val month = solMonth?.trim().orEmpty()
        if (year.isNotEmpty() || month.isNotEmpty()) {
            require(YYYY.matches(year)) { "solYear는 YYYY 형식이어야 합니다." }
            require(MM.matches(month)) { "solMonth는 MM 형식이어야 합니다." }
            return year to month
        }
        val now = LocalDate.now()
        return now.year.toString() to "%02d".format(now.monthValue)
    }

    companion object {
        private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE
        private val YYYYMMDD = Regex("^\\d{8}$")
        private val YYYY = Regex("^\\d{4}$")
        private val MM = Regex("^(0[1-9]|1[0-2])$")
    }

    private fun toFestivalResponse(event: com.sleekydz86.idolglow.eventinfo.domain.FestivalEvent): FestivalEventResponse =
        FestivalEventResponse(
            contentId = event.contentId,
            title = event.title,
            address = event.address,
            eventStartDate = event.eventStartDate,
            eventEndDate = event.eventEndDate,
            thumbnailImageUrl = event.thumbnailImageUrl,
            imageUrl = event.imageUrl,
            mapX = event.mapX,
            mapY = event.mapY,
            phone = event.phone,
            detailUrl = event.detailUrl,
            category = event.category,
            synopsis = event.synopsis,
            source = event.source,
            cast = event.cast,
            runningTime = event.runningTime,
            age = event.age,
            bookingPlaces = event.bookingPlaces,
            introImageUrls = event.introImageUrls,
        )

    private fun daysBetween(start: String, end: String): Long {
        val s = LocalDate.parse(start, DATE_FMT)
        val e = LocalDate.parse(end, DATE_FMT)
        require(!e.isBefore(s)) { "edDate는 stDate 이후여야 합니다." }
        return java.time.temporal.ChronoUnit.DAYS.between(s, e) + 1
    }

    private fun toSpecialDayResponse(day: com.sleekydz86.idolglow.eventinfo.domain.SpecialDayInfo): SpecialDayInfoResponse =
        SpecialDayInfoResponse(
            dateName = day.dateName,
            locDate = day.locDate,
            dateKind = day.dateKind,
            isHoliday = day.isHoliday,
            seq = day.seq,
            source = day.source,
        )
}
