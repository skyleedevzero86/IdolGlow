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

@Tag(name = "Admin Main Image", description = "Admin API for main slide management")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/admin/mim")
class AdminMimController(
    private val mimAdminService: MimAdminService,
    private val webzineImageUploadUseCase: WebzineImageUploadUseCase,
) {
    @Operation(summary = "Find main images")
    @GetMapping
    fun list(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) searchType: String?,
        @RequestParam(required = false) keyword: String?,
    ): ResponseEntity<MimAdminPageResponse> =
        ResponseEntity.ok(mimAdminService.findPage(page, size, searchType, keyword))

    @Operation(summary = "Find main image")
    @GetMapping("/{imageId}")
    fun one(@PathVariable imageId: String) =
        ResponseEntity.ok(mimAdminService.findOne(imageId))

    @Operation(summary = "Create main image")
    @PostMapping
    fun create(@Valid @RequestBody request: UpsertMimRequest): ResponseEntity<*> {
        val created = mimAdminService.create(request)
        return ResponseEntity
            .created(URI.create("/admin/mim/${created.imageId}"))
            .body(created)
    }

    @Operation(summary = "Upload main image")
    @PostMapping("/uploads/images", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadImage(
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<AdminIssueImageUploadResponse> =
        ResponseEntity.ok(webzineImageUploadUseCase.upload(file, "site-content/main-slides"))

    @Operation(summary = "Update main image")
    @PutMapping("/{imageId}")
    fun update(
        @PathVariable imageId: String,
        @Valid @RequestBody request: UpsertMimRequest,
    ) = ResponseEntity.ok(mimAdminService.update(imageId, request))

    @Operation(summary = "Delete main image")
    @DeleteMapping("/{imageId}")
    fun delete(@PathVariable imageId: String): ResponseEntity<Void> {
        mimAdminService.delete(imageId)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }
}
