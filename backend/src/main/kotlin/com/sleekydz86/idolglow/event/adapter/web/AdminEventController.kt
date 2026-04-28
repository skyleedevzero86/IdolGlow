package com.sleekydz86.idolglow.event.ui

import com.sleekydz86.idolglow.event.application.AdminEventService
import com.sleekydz86.idolglow.event.ui.dto.AdminEventDetailResponse
import com.sleekydz86.idolglow.event.ui.dto.AdminEventImageUploadResponse
import com.sleekydz86.idolglow.event.ui.dto.AdminEventPageResponse
import com.sleekydz86.idolglow.event.ui.request.UpsertAdminEventRequest
import com.sleekydz86.idolglow.webzine.application.WebzineImageUploadUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.net.URI

@Tag(name = "관리자 이벤트", description = "Idol Glow 이벤트 문서 등록·수정·삭제 및 목록 API")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/admin/events", "/api/admin/events")
class AdminEventController(
    private val adminEventService: AdminEventService,
    private val webzineImageUploadUseCase: WebzineImageUploadUseCase,
) {
    @Operation(summary = "이벤트 목록 조회")
    @GetMapping
    fun findEvents(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "") query: String,
        @RequestParam(defaultValue = "published") status: String,
    ): ResponseEntity<AdminEventPageResponse> =
        ResponseEntity.ok(adminEventService.findEvents(page, size, query, status))

    @Operation(summary = "이벤트 상세 조회")
    @GetMapping("/{documentId}")
    fun findEvent(
        @PathVariable documentId: String,
    ): ResponseEntity<AdminEventDetailResponse> =
        ResponseEntity.ok(adminEventService.findEvent(documentId))

    @Operation(summary = "이벤트 등록/수정")
    @PostMapping
    fun upsertEvent(
        @Valid @RequestBody request: UpsertAdminEventRequest,
    ): ResponseEntity<AdminEventDetailResponse> {
        val saved = adminEventService.upsertEvent(request.toCommand())
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .location(URI.create("/admin/events/${saved.documentId}"))
            .body(saved)
    }

    @Operation(summary = "이벤트 삭제")
    @DeleteMapping("/{documentId}")
    fun deleteEvent(
        @PathVariable documentId: String,
    ): ResponseEntity<Void> {
        adminEventService.deleteEvent(documentId)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "이벤트 썸네일 업로드")
    @PostMapping("/uploads/images", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadEventImage(
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<AdminEventImageUploadResponse> {
        val uploaded = webzineImageUploadUseCase.upload(file, "site-content/events")
        return ResponseEntity.ok(AdminEventImageUploadResponse.from(uploaded))
    }
}
