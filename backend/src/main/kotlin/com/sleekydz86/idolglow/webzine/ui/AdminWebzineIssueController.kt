package com.sleekydz86.idolglow.webzine.ui

import com.sleekydz86.idolglow.webzine.application.WebzineAdminUseCase
import com.sleekydz86.idolglow.webzine.application.WebzineImageUploadUseCase
import com.sleekydz86.idolglow.webzine.application.dto.AdminIssueArticleResponse
import com.sleekydz86.idolglow.webzine.application.dto.AdminIssueImageUploadResponse
import com.sleekydz86.idolglow.webzine.application.dto.AdminIssuePageResponse
import com.sleekydz86.idolglow.webzine.application.dto.AdminIssueVolumeResponse
import com.sleekydz86.idolglow.webzine.application.dto.CreateWebzineIssueCommand
import com.sleekydz86.idolglow.webzine.application.dto.UpsertWebzineArticleCommand
import com.sleekydz86.idolglow.webzine.application.dto.UpsertWebzineArticleSectionCommand
import com.sleekydz86.idolglow.webzine.ui.request.CreateWebzineIssueRequest
import com.sleekydz86.idolglow.webzine.ui.request.UpsertWebzineArticleRequest
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

@Tag(name = "관리자 웹진", description = "Idol Glow 웹진 호·기사 관리 API")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/admin/issues")
class AdminWebzineIssueController(
    private val webzineAdminUseCase: WebzineAdminUseCase,
    private val webzineImageUploadUseCase: WebzineImageUploadUseCase,
) {

    @Operation(summary = "웹진 호 목록 조회")
    @GetMapping
    fun findIssues(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "5") size: Int,
        @RequestParam(required = false) year: Int?,
        @RequestParam(required = false) month: Int?,
        @RequestParam(required = false) volume: Int?,
    ): ResponseEntity<AdminIssuePageResponse> =
        ResponseEntity.ok(webzineAdminUseCase.findIssues(page, size, year, month, volume))

    @Operation(summary = "웹진 호 등록")
    @PostMapping
    fun createIssue(
        @Valid @RequestBody request: CreateWebzineIssueRequest,
    ): ResponseEntity<AdminIssueVolumeResponse> {
        val created = webzineAdminUseCase.createIssue(request.toCommand())
        return ResponseEntity
            .created(URI.create("/admin/issues/${created.slug}"))
            .body(created)
    }

    @Operation(summary = "Update webzine issue")
    @PutMapping("/{issueSlug}")
    fun updateIssue(
        @PathVariable issueSlug: String,
        @Valid @RequestBody request: CreateWebzineIssueRequest,
    ): ResponseEntity<AdminIssueVolumeResponse> =
        ResponseEntity.ok(webzineAdminUseCase.updateIssue(issueSlug, request.toCommand()))

    @Operation(summary = "웹진 호 단건 조회")
    @GetMapping("/{issueSlug}")
    fun findIssue(@PathVariable issueSlug: String): ResponseEntity<AdminIssueVolumeResponse> =
        ResponseEntity.ok(webzineAdminUseCase.findIssue(issueSlug))

    @Operation(summary = "Delete webzine issue")
    @DeleteMapping("/{issueSlug}")
    fun deleteIssue(@PathVariable issueSlug: String): ResponseEntity<Void> {
        webzineAdminUseCase.deleteIssue(issueSlug)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    @Operation(summary = "웹진 기사 단건 조회")
    @GetMapping("/{issueSlug}/articles/{articleSlug}")
    fun findArticle(
        @PathVariable issueSlug: String,
        @PathVariable articleSlug: String,
    ): ResponseEntity<AdminIssueArticleResponse> =
        ResponseEntity.ok(webzineAdminUseCase.findArticle(issueSlug, articleSlug))

    @Operation(summary = "웹진 기사 등록")
    @PostMapping("/{issueSlug}/articles")
    fun createArticle(
        @PathVariable issueSlug: String,
        @Valid @RequestBody request: UpsertWebzineArticleRequest,
    ): ResponseEntity<AdminIssueArticleResponse> {
        val created = webzineAdminUseCase.createArticle(issueSlug, request.toCommand())
        return ResponseEntity
            .created(URI.create("/admin/issues/$issueSlug/articles/${created.slug}"))
            .body(created)
    }

    @Operation(summary = "웹진 기사 수정")
    @PutMapping("/{issueSlug}/articles/{articleSlug}")
    fun updateArticle(
        @PathVariable issueSlug: String,
        @PathVariable articleSlug: String,
        @Valid @RequestBody request: UpsertWebzineArticleRequest,
    ): ResponseEntity<AdminIssueArticleResponse> =
        ResponseEntity.ok(webzineAdminUseCase.updateArticle(issueSlug, articleSlug, request.toCommand()))

    @Operation(summary = "웹진 기사 삭제")
    @DeleteMapping("/{issueSlug}/articles/{articleSlug}")
    fun deleteArticle(
        @PathVariable issueSlug: String,
        @PathVariable articleSlug: String,
    ): ResponseEntity<Void> {
        webzineAdminUseCase.deleteArticle(issueSlug, articleSlug)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    @Operation(summary = "웹진 이미지 업로드")
    @PostMapping("/uploads/images", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadImage(
        @RequestPart("file") file: MultipartFile,
        @RequestParam(required = false) folder: String?,
    ): ResponseEntity<AdminIssueImageUploadResponse> =
        ResponseEntity.ok(webzineImageUploadUseCase.upload(file, folder))

    private fun CreateWebzineIssueRequest.toCommand(): CreateWebzineIssueCommand =
        CreateWebzineIssueCommand(
            volume = volume,
            issueDate = issueDate,
            coverImageUrl = coverImageUrl,
            teaser = teaser,
        )

    private fun UpsertWebzineArticleRequest.toCommand(): UpsertWebzineArticleCommand =
        UpsertWebzineArticleCommand(
            title = title,
            kicker = kicker,
            summary = summary,
            category = category,
            formatLabel = formatLabel,
            heroImageUrl = heroImageUrl,
            cardImageUrl = cardImageUrl,
            galleryImageUrls = galleryImageUrls,
            tags = tags,
            authorName = authorName,
            authorEmail = authorEmail,
            creditLine = creditLine,
            highlightQuote = highlightQuote,
            sections = sections.map {
                UpsertWebzineArticleSectionCommand(
                    heading = it.heading,
                    body = it.body,
                    note = it.note,
                )
            },
        )
}
