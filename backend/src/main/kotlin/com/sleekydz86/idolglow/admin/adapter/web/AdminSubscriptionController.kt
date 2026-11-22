package com.sleekydz86.idolglow.admin.adapter.web

import com.sleekydz86.idolglow.admin.adapter.web.dto.AdminSubscriptionOverviewResponse
import com.sleekydz86.idolglow.admin.adapter.web.dto.AdminSubscriptionScheduleResponse
import com.sleekydz86.idolglow.admin.adapter.web.request.UpsertSubscriptionDispatchScheduleRequest
import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetPageResult
import com.sleekydz86.idolglow.subscription.application.dto.AdminDateRange
import com.sleekydz86.idolglow.subscription.application.dto.AdminEmailSubscriberResult
import com.sleekydz86.idolglow.subscription.application.dto.AdminMailSendLogDetailResult
import com.sleekydz86.idolglow.subscription.application.dto.AdminMailSendLogResult
import com.sleekydz86.idolglow.subscription.application.dto.AdminRecommendationSubscriberResult
import com.sleekydz86.idolglow.subscription.application.dto.AdminSubscriptionDispatchDetailResult
import com.sleekydz86.idolglow.subscription.application.dto.DispatchHistorySearchCondition
import com.sleekydz86.idolglow.subscription.application.dto.DispatchRecipientSearchCondition
import com.sleekydz86.idolglow.subscription.application.dto.EmailSubscriberSearchCondition
import com.sleekydz86.idolglow.subscription.application.dto.MailSendLogSearchCondition
import com.sleekydz86.idolglow.subscription.application.dto.RecommendationSubscriberSearchCondition
import com.sleekydz86.idolglow.subscription.application.dto.SubscriberStatusFilter
import com.sleekydz86.idolglow.subscription.application.dto.SubscriptionDispatchResult
import com.sleekydz86.idolglow.subscription.application.port.`in`.SubscriptionAdminQueryUseCase
import com.sleekydz86.idolglow.subscription.application.port.`in`.SubscriptionAdminUseCase
import com.sleekydz86.idolglow.subscription.domain.MailSendKind
import com.sleekydz86.idolglow.subscription.domain.MailSendStatus
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailPreferredBasis
import com.sleekydz86.idolglow.subscription.domain.SubscriptionAudience
import com.sleekydz86.idolglow.subscription.domain.SubscriptionContentType
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchRecipientStatus
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import com.sleekydz86.idolglow.admin.adapter.web.dto.toWebResponse as subscriptionToWebResponse

@Tag(
    name = "관리자 구독·발송",
    description = "이메일 구독자 및 소식지·웹진 발송 이력 조회 API",
)
@RestController
@RequestMapping("/admin/subscriptions")
class AdminSubscriptionController(
    private val subscriptionAdminUseCase: SubscriptionAdminUseCase,
    private val subscriptionAdminQueryUseCase: SubscriptionAdminQueryUseCase,
) {
    @Operation(
        summary = "구독·발송 개요 조회",
        description = "구독자 통계와 최근 발송 이력을 관리자 구독 화면용으로 반환합니다.",
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun overview(
        @RequestParam(defaultValue = "1") subscriberPage: Int,
        @RequestParam(defaultValue = "10") subscriberSize: Int,
        @RequestParam(defaultValue = "1") dispatchPage: Int,
        @RequestParam(defaultValue = "10") dispatchSize: Int,
    ): ResponseEntity<AdminSubscriptionOverviewResponse> =
        ResponseEntity.ok(
            subscriptionAdminUseCase
                .findOverview(
                    subscriberPage = subscriberPage,
                    subscriberSize = subscriberSize,
                    dispatchPage = dispatchPage,
                    dispatchSize = dispatchSize,
                ).subscriptionToWebResponse(),
        )

    @Operation(summary = "예약 발송 스케줄 저장")
    @PutMapping("/schedules/{contentType}")
    @PreAuthorize("hasRole('ADMIN')")
    fun upsertSchedule(
        @PathVariable contentType: SubscriptionContentType,
        @Valid @RequestBody request: UpsertSubscriptionDispatchScheduleRequest,
    ): ResponseEntity<AdminSubscriptionScheduleResponse> =
        ResponseEntity.ok(subscriptionAdminUseCase.upsertDispatchSchedule(request.toCommand(contentType)).subscriptionToWebResponse())

    @Operation(
        summary = "소식지·웹진 이메일 구독자 목록",
        description = "이메일 일부로 검색하고 구독 대상·상태로 거릅니다. 이메일은 가려서 돌려줍니다. page 는 0부터 시작합니다.",
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/email-subscribers")
    @PreAuthorize("hasRole('ADMIN')")
    fun emailSubscribers(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) audience: SubscriptionAudience?,
        @RequestParam(required = false) status: SubscriberStatusFilter?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): OffsetPageResult<AdminEmailSubscriberResult> =
        subscriptionAdminQueryUseCase.findEmailSubscribers(
            EmailSubscriberSearchCondition(keyword = keyword, audience = audience, status = status),
            OffsetPageRequest.of(page, size),
        )

    @Operation(
        summary = "상품 추천 메일 구독자 목록",
        description = "이메일·닉네임 일부로 검색하고 상태·추천 기준으로 거릅니다. 이메일은 가려서 돌려줍니다.",
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/recommendation-subscribers")
    @PreAuthorize("hasRole('ADMIN')")
    fun recommendationSubscribers(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) status: SubscriberStatusFilter?,
        @RequestParam(required = false) basis: ProductRecommendationMailPreferredBasis?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): OffsetPageResult<AdminRecommendationSubscriberResult> =
        subscriptionAdminQueryUseCase.findRecommendationSubscribers(
            RecommendationSubscriberSearchCondition(keyword = keyword, status = status, basis = basis),
            OffsetPageRequest.of(page, size),
        )

    @Operation(
        summary = "발송 이력 목록",
        description = "콘텐츠 제목·슬러그로 검색하고 유형·상태·발송일로 거릅니다. 행마다 발송·실패·건너뜀 수를 함께 돌려줍니다.",
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/dispatches")
    @PreAuthorize("hasRole('ADMIN')")
    @Suppress("LongParameterList")
    fun dispatches(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) contentType: SubscriptionContentType?,
        @RequestParam(required = false) status: SubscriptionDispatchStatus?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): OffsetPageResult<SubscriptionDispatchResult> =
        subscriptionAdminQueryUseCase.findDispatches(
            DispatchHistorySearchCondition(
                keyword = keyword,
                contentType = contentType,
                status = status,
                range = AdminDateRange.of(from, to),
            ),
            OffsetPageRequest.of(page, size),
        )

    @Operation(
        summary = "발송 이력 상세",
        description = "발송 이력, 수신자 목록(이메일 검색·상태 필터·페이지), 대표 메일 내용을 돌려줍니다.",
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/dispatches/{dispatchId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun dispatchDetail(
        @PathVariable dispatchId: Long,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) status: SubscriptionDispatchRecipientStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): AdminSubscriptionDispatchDetailResult =
        subscriptionAdminQueryUseCase.findDispatchDetail(
            DispatchRecipientSearchCondition(dispatchId = dispatchId, keyword = keyword, status = status),
            OffsetPageRequest.of(page, size),
        )

    @Operation(
        summary = "메일 발송 기록 목록",
        description = "모든 발송 메일 기록. 종류·상태·기간·수신 이메일로 거릅니다. 90일이 지나면 지워집니다.",
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/mail-logs")
    @PreAuthorize("hasRole('ADMIN')")
    @Suppress("LongParameterList")
    fun mailLogs(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) kind: MailSendKind?,
        @RequestParam(required = false) status: MailSendStatus?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): OffsetPageResult<AdminMailSendLogResult> =
        subscriptionAdminQueryUseCase.findMailLogs(
            MailSendLogSearchCondition(
                keyword = keyword,
                kind = kind,
                status = status,
                range = AdminDateRange.of(from, to),
            ),
            OffsetPageRequest.of(page, size),
        )

    @Operation(summary = "메일 발송 기록 상세", description = "제목과 본문을 돌려줍니다. 본문 속 토큰은 가려져 있습니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/mail-logs/{mailLogId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun mailLog(
        @PathVariable mailLogId: Long,
    ): AdminMailSendLogDetailResult = subscriptionAdminQueryUseCase.findMailLog(mailLogId)
}
