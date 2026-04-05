package com.sleekydz86.idolglow.webzine.ui

import com.sleekydz86.idolglow.webzine.application.WebzineAdminService
import com.sleekydz86.idolglow.webzine.application.WebzineImageUploadService
import com.sleekydz86.idolglow.webzine.application.dto.AdminIssueArticleResponse
import com.sleekydz86.idolglow.webzine.application.dto.AdminIssueImageUploadResponse
import com.sleekydz86.idolglow.webzine.application.dto.AdminIssuePageResponse
import com.sleekydz86.idolglow.webzine.application.dto.AdminIssueVolumeResponse
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

@Tag(name = "Admin Webzine", description = "웹진 ACC 호별보기 관리자 API")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/admin/issues")
class AdminWebzineIssueController(
    private val webzineAdminService: WebzineAdminService,
    private val webzineImageUploadService: WebzineImageUploadService,
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
        ResponseEntity.ok(webzineAdminService.findIssues(page, size, year, month, volume))

    @Operation(summary = "웹진 호 등록")
    @PostMapping
    fun createIssue(
        @Valid @RequestBody request: CreateWebzineIssueRequest,
    ): ResponseEntity<AdminIssueVolumeResponse> {
        val created = webzineAdminService.createIssue(request)
        return ResponseEntity
            .created(URI.create("/admin/issues/${created.slug}"))
            .body(created)
    }

    @Operation(summary = "웹진 호 수정")
    @PutMapping("/{issueSlug}")
    fun updateIssue(
        @PathVariable issueSlug: String,
        @Valid @RequestBody request: CreateWebzineIssueRequest,
    ): ResponseEntity<AdminIssueVolumeResponse> =
        ResponseEntity.ok(webzineAdminService.updateIssue(issueSlug, request))

    @Operation(summary = "웹진 호 상세 조회")
    @GetMapping("/{issueSlug}")
    fun findIssue(@PathVariable issueSlug: String): ResponseEntity<AdminIssueVolumeResponse> =
        ResponseEntity.ok(webzineAdminService.findIssue(issueSlug))

    @Operation(summary = "웹진 호 삭제")
    @DeleteMapping("/{issueSlug}")
    fun deleteIssue(@PathVariable issueSlug: String): ResponseEntity<Void> {
        webzineAdminService.deleteIssue(issueSlug)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    @Operation(summary = "웹진 기사 상세 조회")
    @GetMapping("/{issueSlug}/articles/{articleSlug}")
    fun findArticle(
        @PathVariable issueSlug: String,
        @PathVariable articleSlug: String,
    ): ResponseEntity<AdminIssueArticleResponse> =
        ResponseEntity.ok(webzineAdminService.findArticle(issueSlug, articleSlug))

    @Operation(summary = "웹진 기사 등록")
    @PostMapping("/{issueSlug}/articles")
    fun createArticle(
        @PathVariable issueSlug: String,
        @Valid @RequestBody request: UpsertWebzineArticleRequest,
    ): ResponseEntity<AdminIssueArticleResponse> {
        val created = webzineAdminService.createArticle(issueSlug, request)
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
        ResponseEntity.ok(webzineAdminService.updateArticle(issueSlug, articleSlug, request))

    @Operation(summary = "웹진 기사 삭제")
    @DeleteMapping("/{issueSlug}/articles/{articleSlug}")
    fun deleteArticle(
        @PathVariable issueSlug: String,
        @PathVariable articleSlug: String,
    ): ResponseEntity<Void> {
        webzineAdminService.deleteArticle(issueSlug, articleSlug)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    @Operation(summary = "웹진 이미지 업로드")
    @PostMapping("/uploads/images", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadImage(
        @RequestPart("file") file: MultipartFile,
        @RequestParam(required = false) folder: String?,
    ): ResponseEntity<AdminIssueImageUploadResponse> =
        ResponseEntity.ok(webzineImageUploadService.upload(file, folder))
}
