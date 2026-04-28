package com.sleekydz86.idolglow.bnr.ui

import com.sleekydz86.idolglow.bnr.application.BnrAdminService
import com.sleekydz86.idolglow.bnr.application.dto.BnrAdminPageResponse
import com.sleekydz86.idolglow.bnr.application.dto.UpsertBnrRequest
import com.sleekydz86.idolglow.webzine.application.WebzineImageUploadUseCase
import com.sleekydz86.idolglow.webzine.application.dto.AdminIssueImageUploadResponse
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

@Tag(name = "관리자 배너", description = "사이트 배너(tb_*) 등록·조회·수정·삭제 API")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/admin/bnr")
class AdminBnrController(
    private val bnrAdminService: BnrAdminService,
    private val webzineImageUploadUseCase: WebzineImageUploadUseCase,
) {
    @Operation(summary = "배너 목록 조회")
    @GetMapping
    fun list(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) searchType: String?,
        @RequestParam(required = false) keyword: String?,
    ): ResponseEntity<BnrAdminPageResponse> =
        ResponseEntity.ok(bnrAdminService.findPage(page, size, searchType, keyword))

    @Operation(summary = "배너 단건 조회")
    @GetMapping("/{bannerId}")
    fun one(@PathVariable bannerId: String) =
        ResponseEntity.ok(bnrAdminService.findOne(bannerId))

    @Operation(summary = "배너 등록")
    @PostMapping
    fun create(@Valid @RequestBody request: UpsertBnrRequest): ResponseEntity<*> {
        val created = bnrAdminService.create(request)
        return ResponseEntity
            .created(URI.create("/admin/bnr/${created.bannerId}"))
            .body(created)
    }

    @Operation(summary = "배너 이미지 업로드")
    @PostMapping("/uploads/images", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadImage(
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<AdminIssueImageUploadResponse> =
        ResponseEntity.ok(webzineImageUploadUseCase.upload(file, "site-content/banners"))

    @Operation(summary = "배너 수정")
    @PutMapping("/{bannerId}")
    fun update(
        @PathVariable bannerId: String,
        @Valid @RequestBody request: UpsertBnrRequest,
    ) = ResponseEntity.ok(bnrAdminService.update(bannerId, request))

    @Operation(summary = "배너 삭제")
    @DeleteMapping("/{bannerId}")
    fun delete(@PathVariable bannerId: String): ResponseEntity<Void> {
        bnrAdminService.delete(bannerId)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }
}
