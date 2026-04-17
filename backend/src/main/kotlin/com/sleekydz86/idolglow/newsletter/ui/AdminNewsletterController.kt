package com.sleekydz86.idolglow.newsletter.ui

import com.sleekydz86.idolglow.newsletter.application.NewsletterAdminUseCase
import com.sleekydz86.idolglow.newsletter.application.dto.AdminNewsletterDetailResponse
import com.sleekydz86.idolglow.newsletter.application.dto.AdminNewsletterImageUploadResponse
import com.sleekydz86.idolglow.newsletter.application.dto.AdminNewsletterPageResponse
import com.sleekydz86.idolglow.newsletter.application.dto.UpsertNewsletterCommand
import com.sleekydz86.idolglow.newsletter.ui.request.UpsertNewsletterRequest
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
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.net.URI

@Tag(name = "관리자 소식지", description = "Idol Glow 소식지 등록·조회·수정·삭제 API")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/admin/newsletters")
class AdminNewsletterController(
    private val newsletterAdminUseCase: NewsletterAdminUseCase,
    private val webzineImageUploadUseCase: WebzineImageUploadUseCase,
) {

    @Operation(summary = "소식지 목록 조회")
    @GetMapping
    fun findNewsletters(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ResponseEntity<AdminNewsletterPageResponse> =
        ResponseEntity.ok(newsletterAdminUseCase.findNewsletters(page, size))

    @Operation(summary = "소식지 단건 조회")
    @GetMapping("/{newsletterSlug}")
    fun findNewsletter(
        @PathVariable newsletterSlug: String,
    ): ResponseEntity<AdminNewsletterDetailResponse> =
        ResponseEntity.ok(newsletterAdminUseCase.findNewsletter(newsletterSlug))

    @Operation(summary = "소식지 등록")
    @PostMapping
    fun createNewsletter(
        @Valid @RequestBody request: UpsertNewsletterRequest,
    ): ResponseEntity<AdminNewsletterDetailResponse> {
        val created = newsletterAdminUseCase.createNewsletter(request.toCommand())
        return ResponseEntity
            .created(URI.create("/admin/newsletters/${created.slug}"))
            .body(created)
    }

    @Operation(summary = "소식지 수정")
    @PutMapping("/{newsletterSlug}")
    fun updateNewsletter(
        @PathVariable newsletterSlug: String,
        @Valid @RequestBody request: UpsertNewsletterRequest,
    ): ResponseEntity<AdminNewsletterDetailResponse> =
        ResponseEntity.ok(newsletterAdminUseCase.updateNewsletter(newsletterSlug, request.toCommand()))

    @Operation(summary = "소식지 삭제")
    @DeleteMapping("/{newsletterSlug}")
    fun deleteNewsletter(
        @PathVariable newsletterSlug: String,
    ): ResponseEntity<Void> {
        newsletterAdminUseCase.deleteNewsletter(newsletterSlug)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    @Operation(summary = "소식지 이미지 업로드")
    @PostMapping("/uploads/images", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadNewsletterImage(
        @RequestPart("file") file: MultipartFile,
        @RequestParam(required = false) folder: String?,
    ): ResponseEntity<AdminNewsletterImageUploadResponse> {
        val upload = webzineImageUploadUseCase.upload(file, folder ?: "newsletters/common")
        return ResponseEntity.ok(
            AdminNewsletterImageUploadResponse(
                url = upload.url,
                objectKey = upload.objectKey,
                contentType = upload.contentType,
                size = upload.size,
            )
        )
    }

    private fun UpsertNewsletterRequest.toCommand(): UpsertNewsletterCommand =
        UpsertNewsletterCommand(
            title = title,
            categoryLabel = categoryLabel,
            publishedAt = publishedAt,
            imageUrl = imageUrl,
            tags = tags,
            summary = summary,
            paragraphs = paragraphs,
        )
}
