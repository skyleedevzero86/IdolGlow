package com.sleekydz86.idolglow.mim.ui

import com.sleekydz86.idolglow.mim.application.MimAdminService
import com.sleekydz86.idolglow.mim.application.dto.MimAdminPageResponse
import com.sleekydz86.idolglow.mim.application.dto.UpsertMimRequest
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

@Tag(name = "관리자 메인 이미지", description = "메인 슬라이드(tb_*) 등록·조회·수정·삭제 API")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/admin/mim")
class AdminMimController(
    private val mimAdminService: MimAdminService,
    private val webzineImageUploadUseCase: WebzineImageUploadUseCase,
) {
    @Operation(summary = "메인 이미지 목록 조회")
    @GetMapping
    fun list(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) searchType: String?,
        @RequestParam(required = false) keyword: String?,
    ): ResponseEntity<MimAdminPageResponse> =
        ResponseEntity.ok(mimAdminService.findPage(page, size, searchType, keyword))

    @Operation(summary = "메인 이미지 단건 조회")
    @GetMapping("/{imageId}")
    fun one(@PathVariable imageId: String) =
        ResponseEntity.ok(mimAdminService.findOne(imageId))

    @Operation(summary = "메인 이미지 등록")
    @PostMapping
    fun create(@Valid @RequestBody request: UpsertMimRequest): ResponseEntity<*> {
        val created = mimAdminService.create(request)
        return ResponseEntity
            .created(URI.create("/admin/mim/${created.imageId}"))
            .body(created)
    }

    @Operation(summary = "메인 이미지 업로드")
    @PostMapping("/uploads/images", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadImage(
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<AdminIssueImageUploadResponse> =
        ResponseEntity.ok(webzineImageUploadUseCase.upload(file, "site-content/main-slides"))

    @Operation(summary = "메인 이미지 수정")
    @PutMapping("/{imageId}")
    fun update(
        @PathVariable imageId: String,
        @Valid @RequestBody request: UpsertMimRequest,
    ) = ResponseEntity.ok(mimAdminService.update(imageId, request))

    @Operation(summary = "메인 이미지 삭제")
    @DeleteMapping("/{imageId}")
    fun delete(@PathVariable imageId: String): ResponseEntity<Void> {
        mimAdminService.delete(imageId)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }
}
