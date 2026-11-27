package com.sleekydz86.idolglow.mypage.adapter.web

import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import com.sleekydz86.idolglow.mypage.application.MyDashboardService
import com.sleekydz86.idolglow.mypage.application.dto.MyDashboardResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "회원 대시보드", description = "회원 마이페이지에 보여 줄 본인 집계 API")
@RestController
class MyDashboardController(
    private val myDashboardService: MyDashboardService,
) {
    @Operation(
        summary = "내 대시보드 집계",
        description = "본인이 신고한 지역사회 게시글 수와 최근 90일(오늘 포함, 서울 날짜) 방문한 날 수를 줍니다. 로그인하지 않으면 401입니다.",
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me/dashboard")
    fun summary(
        @LoginUser userId: Long,
    ): MyDashboardResult = myDashboardService.summarize(userId)
}
