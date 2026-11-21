package com.sleekydz86.idolglow.eventinfo.adapter.web

import com.sleekydz86.idolglow.eventinfo.application.EventInterestService
import com.sleekydz86.idolglow.eventinfo.application.EventInterestView
import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "공연 관심 등록", description = "관심 등록한 공연의 일정·장소·상태가 바뀌면 알림을 받는다")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/event-info/interests")
class EventInterestController(
    private val eventInterestService: EventInterestService,
) {
    @Operation(summary = "공연 관심 등록", description = "이미 등록돼 있으면 그대로 둔다")
    @PostMapping("/{globalId}")
    fun register(
        @LoginUser userId: Long,
        @PathVariable globalId: String,
    ): ResponseEntity<EventInterestView> = ResponseEntity.ok(eventInterestService.register(userId, globalId))

    @Operation(summary = "공연 관심 해제")
    @DeleteMapping("/{globalId}")
    fun unregister(
        @LoginUser userId: Long,
        @PathVariable globalId: String,
    ): ResponseEntity<Unit> {
        eventInterestService.unregister(userId, globalId)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "내 관심 공연 목록")
    @GetMapping
    fun list(
        @LoginUser userId: Long,
    ): ResponseEntity<List<EventInterestView>> = ResponseEntity.ok(eventInterestService.list(userId))
}
