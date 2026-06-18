package com.sleekydz86.idolglow.productpackage.admin.adapter.web

import com.sleekydz86.idolglow.productpackage.admin.adapter.web.request.CreateReservationSlotsRequest
import com.sleekydz86.idolglow.productpackage.admin.adapter.web.request.UpdateAdminMarkdownRequest
import com.sleekydz86.idolglow.productpackage.admin.adapter.web.request.toCommand
import com.sleekydz86.idolglow.productpackage.admin.application.AdminCatalogService
import com.sleekydz86.idolglow.productpackage.admin.application.dto.AdminReservationSlotResponse
import com.sleekydz86.idolglow.productpackage.option.adapter.web.request.CreateOptionRequest
import com.sleekydz86.idolglow.productpackage.option.adapter.web.request.toCommand
import com.sleekydz86.idolglow.productpackage.option.application.OptionQueryService
import com.sleekydz86.idolglow.productpackage.option.application.dto.OptionPageResponse
import com.sleekydz86.idolglow.productpackage.product.adapter.web.request.CreateProductRequest
import com.sleekydz86.idolglow.productpackage.product.adapter.web.request.toCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "관리자 카탈로그", description = "관리자 상품·옵션·슬롯 관리 API")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/admin")
class AdminCatalogController(
    private val adminCatalogService: AdminCatalogService,
    private val optionQueryService: OptionQueryService,
) {
    @Operation(summary = "옵션 검색")
    @GetMapping("/options")
    fun searchOptions(
        @RequestParam(required = false) q: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ResponseEntity<OptionPageResponse> {
        val trimmed = q?.trim().orEmpty()
        return ResponseEntity.ok(
            optionQueryService.searchOptions(
                q = trimmed.ifEmpty { null },
                page = page,
                size = size,
            ),
        )
    }

    @Operation(summary = "상품 예약 슬롯 목록 조회")
    @GetMapping("/products/{productId}/slots")
    fun findSlots(
        @PathVariable productId: Long,
    ): ResponseEntity<List<AdminReservationSlotResponse>> = ResponseEntity.ok(adminCatalogService.findSlots(productId))

    @Operation(summary = "상품 예약 슬롯 생성")
    @PostMapping("/products/{productId}/slots")
    fun createSlots(
        @PathVariable productId: Long,
        @Valid @RequestBody request: CreateReservationSlotsRequest,
    ): ResponseEntity<List<AdminReservationSlotResponse>> = ResponseEntity.ok(adminCatalogService.createSlots(productId, request.toCommand()))

    @Operation(summary = "상품 수정")
    @PutMapping("/products/{productId}")
    fun updateProduct(
        @PathVariable productId: Long,
        @Valid @RequestBody request: CreateProductRequest,
    ): ResponseEntity<Void> {
        adminCatalogService.updateProduct(productId, request.toCommand())
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "옵션 수정")
    @PutMapping("/options/{optionId}")
    fun updateOption(
        @PathVariable optionId: Long,
        @Valid @RequestBody request: CreateOptionRequest,
    ): ResponseEntity<Void> {
        adminCatalogService.updateOption(optionId, request.toCommand())
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "슬롯 메모 수정")
    @PatchMapping("/slots/{slotId}/note")
    fun updateSlotNote(
        @PathVariable slotId: Long,
        @RequestBody request: UpdateAdminMarkdownRequest,
    ): ResponseEntity<AdminReservationSlotResponse> = ResponseEntity.ok(adminCatalogService.updateSlotNote(slotId, request.markdown))

    @Operation(summary = "슬롯 삭제")
    @DeleteMapping("/slots/{slotId}")
    fun deleteSlot(
        @PathVariable slotId: Long,
    ): ResponseEntity<Void> {
        adminCatalogService.deleteSlot(slotId)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "상품 삭제")
    @DeleteMapping("/products/{productId}")
    fun deleteProduct(
        @PathVariable productId: Long,
    ): ResponseEntity<Void> {
        adminCatalogService.deleteProduct(productId)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "옵션 삭제")
    @DeleteMapping("/options/{optionId}")
    fun deleteOption(
        @PathVariable optionId: Long,
    ): ResponseEntity<Void> {
        adminCatalogService.deleteOption(optionId)
        return ResponseEntity.noContent().build()
    }
}
