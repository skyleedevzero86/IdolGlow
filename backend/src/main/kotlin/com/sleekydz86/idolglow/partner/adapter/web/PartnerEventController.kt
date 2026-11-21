package com.sleekydz86.idolglow.partner.adapter.web

import com.sleekydz86.idolglow.global.infrastructure.exception.ExceptionResponse
import com.sleekydz86.idolglow.global.infrastructure.exception.partner.PartnerExceptionType
import com.sleekydz86.idolglow.global.infrastructure.exception.partner.PartnerRateLimitedException
import com.sleekydz86.idolglow.partner.application.PartnerEventFeedService
import com.sleekydz86.idolglow.partner.application.dto.PartnerEventItem
import com.sleekydz86.idolglow.partner.application.dto.PartnerEventPage
import com.sleekydz86.idolglow.partner.application.dto.PartnerFeedQuery
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(
    name = "파트너 메타 API v1",
    description = "여행사·숙소 파트너용 공연 메타 변경분 조회. X-Partner-Key 헤더 필요. v2가 나오면 v1은 6개월 유지",
)
@RestController
@RequestMapping("/partner/v1")
class PartnerEventController(
    private val partnerEventFeedService: PartnerEventFeedService,
) {
    @Operation(
        summary = "공연 변경분 조회",
        description = "수정 시각과 글로벌 ID 순서의 커서로 받는다. 응답의 nextCursor를 다음 호출에 넘기면 중복·누락 없이 이어 받는다",
    )
    @GetMapping("/events")
    fun events(
        @Parameter(description = "파트너 키") @RequestHeader(name = KEY_HEADER, required = false) key: String?,
        @Parameter(description = "이 시각 이후 변경분. 예: 2026-11-21T09:00:00+09:00")
        @RequestParam(required = false) updatedSince: String?,
        @Parameter(description = "이전 응답의 nextCursor") @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "50") size: Int,
        @Parameter(description = "언어. 계약하지 않은 언어는 400") @RequestParam(required = false) lang: String?,
    ): PartnerEventPage = partnerEventFeedService.feed(key, PartnerFeedQuery(updatedSince, cursor, size, lang))

    @Operation(
        summary = "외부 ID로 공연 찾기",
        description = "KOPIS·KTO·SJW·CULTURE는 공급처 원본 ID, WIKIDATA는 Q로 시작하는 위키데이터 ID. 모르는 체계는 400, 없으면 404",
    )
    @GetMapping("/events/by-external-id")
    fun eventByExternalId(
        @Parameter(description = "파트너 키") @RequestHeader(name = KEY_HEADER, required = false) key: String?,
        @Parameter(description = "외부 ID 체계. KOPIS, KTO, SJW, CULTURE, WIKIDATA") @RequestParam scheme: String,
        @Parameter(description = "외부 ID. 예: PF123456, Q12345") @RequestParam id: String,
        @RequestParam(required = false) lang: String?,
    ): PartnerEventItem = partnerEventFeedService.eventByExternalId(key, scheme, id, lang)

    @Operation(summary = "공연 한 건 조회", description = "병합된 공연은 status=MERGED와 이어받은 mergedInto를 준다")
    @GetMapping("/events/{globalId}")
    fun event(
        @Parameter(description = "파트너 키") @RequestHeader(name = KEY_HEADER, required = false) key: String?,
        @PathVariable globalId: String,
        @RequestParam(required = false) lang: String?,
    ): PartnerEventItem = partnerEventFeedService.event(key, globalId, lang)

    @ExceptionHandler(PartnerRateLimitedException::class)
    fun rateLimited(exception: PartnerRateLimitedException): ResponseEntity<ExceptionResponse> {
        val type = PartnerExceptionType.RATE_LIMITED
        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .header(HttpHeaders.RETRY_AFTER, exception.retryAfterSeconds.toString())
            .body(ExceptionResponse(type::class.simpleName ?: type.errorCode, type.errorCode, type.message))
    }

    companion object {
        const val KEY_HEADER: String = "X-Partner-Key"
    }
}
