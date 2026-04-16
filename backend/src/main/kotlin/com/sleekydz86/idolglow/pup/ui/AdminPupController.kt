package com.sleekydz86.idolglow.pup.ui

import com.sleekydz86.idolglow.pup.application.PupAdminService
import com.sleekydz86.idolglow.pup.application.dto.PupAdminPageResponse
import com.sleekydz86.idolglow.pup.application.dto.UpsertPupRequest
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

@Tag(name = "Admin Popup", description = "Admin API for popup management")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/admin/pup")
class AdminPupController(
    private val pupAdminService: PupAdminService,
    private val webzineImageUploadUseCase: WebzineImageUploadUseCase,
) {
    @Operation(summary = "Find popups")
    @GetMapping
    fun list(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) searchType: String?,
        @RequestParam(required = false) keyword: String?,
    ): ResponseEntity<PupAdminPageResponse> =
        ResponseEntity.ok(pupAdminService.findPage(page, size, searchType, keyword))

    @Operation(summary = "Find popup")
    @GetMapping("/{popupId}")
    fun one(@PathVariable popupId: String) =
        ResponseEntity.ok(pupAdminService.findOne(popupId))

    @Operation(summary = "Create popup")
    @PostMapping
    fun create(@Valid @RequestBody request: UpsertPupRequest): ResponseEntity<*> {
        val created = pupAdminService.create(request)
        return ResponseEntity
            .created(URI.create("/admin/pup/${created.popupId}"))
            .body(created)
    }

    @Operation(summary = "Upload popup image")
    @PostMapping("/uploads/images", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadImage(
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<AdminIssueImageUploadResponse> =
        ResponseEntity.ok(webzineImageUploadUseCase.upload(file, "site-content/popups"))

    @Operation(summary = "Update popup")
    @PutMapping("/{popupId}")
    fun update(
        @PathVariable popupId: String,
        @Valid @RequestBody request: UpsertPupRequest,
    ) = ResponseEntity.ok(pupAdminService.update(popupId, request))

    @Operation(summary = "Delete popup")
    @DeleteMapping("/{popupId}")
    fun delete(@PathVariable popupId: String): ResponseEntity<Void> {
        pupAdminService.delete(popupId)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }
}
