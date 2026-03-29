package com.sleekydz86.idolglow.mypage.application

import com.sleekydz86.idolglow.mypage.application.dto.MypagePrimaryPromoResponse
import com.sleekydz86.idolglow.mypage.application.dto.MypagePromoStripResponse
import com.sleekydz86.idolglow.mypage.application.dto.MypageSecondaryPromoResponse
import com.sleekydz86.idolglow.productpackage.discovery.application.ProductDiscoveryService
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationQueryService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MypagePromoService(
    private val productDiscoveryService: ProductDiscoveryService,
    private val reservationQueryService: ReservationQueryService,
) {

    fun promoStrip(userId: Long): MypagePromoStripResponse =
        MypagePromoStripResponse(
            primary = primaryPromo(userId),
            secondary = secondaryPromo(userId),
        )

    fun primaryPromo(userId: Long): MypagePrimaryPromoResponse {
        val pick = productDiscoveryService.findRecommendedProducts(userId, 1).firstOrNull()
        if (pick != null) {
            return MypagePrimaryPromoResponse(
                variant = "PICK",
                textBeforeHighlight = "회원님을 위한 추천 패키지 ",
                highlight = truncateName(pick.name),
                textAfterHighlight = " 살펴보기",
                href = "/articles",
                ctaLabel = "둘러보기 →",
            )
        }
        return defaultPromo()
    }

    private fun defaultPromo(): MypagePrimaryPromoResponse =
        MypagePrimaryPromoResponse(
            variant = "DEFAULT",
            textBeforeHighlight = "아티클부터 이벤트까지, ",
            highlight = "IDOL GLOW",
            textAfterHighlight = "와 함께하는 문화 큐레이션",
            href = "/articles",
            ctaLabel = "둘러보기 →",
        )

    private fun truncateName(raw: String, max: Int = 24): String {
        val t = raw.trim()
        return if (t.length <= max) t else t.take(max - 1) + "…"
    }

    private fun secondaryPromo(userId: Long): MypageSecondaryPromoResponse {
        val all = reservationQueryService.findReservationsByUser(userId)
        val upcoming = reservationQueryService.findUpcomingReservationsByUser(userId)
        val cap = 99
        return when {
            upcoming.isNotEmpty() ->
                MypageSecondaryPromoResponse(
                    variant = "UPCOMING",
                    textBeforeStrong = "방문 예정인 ",
                    strong = "패키지 일정",
                    textAfterStrong = "을 확인하고 준비해 보세요.",
                    metricValue = upcoming.size.coerceAtMost(cap),
                    metricUnit = "다가오는 예약",
                    href = "/mypage#upcoming-strip",
                )

            all.isNotEmpty() ->
                MypageSecondaryPromoResponse(
                    variant = "HISTORY",
                    textBeforeStrong = "이용·예약 내역과 ",
                    strong = "리뷰",
                    textAfterStrong = "를 함께 관리해 보세요.",
                    metricValue = all.size.coerceAtMost(cap),
                    metricUnit = "누적 예약",
                    href = "/mypage#recent-bookings",
                )

            else ->
                MypageSecondaryPromoResponse(
                    variant = "EMPTY",
                    textBeforeStrong = "헤어·메이크업 ",
                    strong = "GLOW 패키지",
                    textAfterStrong = " 첫 예약으로 스튜디오 경험을 시작해 보세요.",
                    metricValue = 0,
                    metricUnit = "건 예약",
                    href = "/mypage#recent-bookings",
                )
        }
    }
}
