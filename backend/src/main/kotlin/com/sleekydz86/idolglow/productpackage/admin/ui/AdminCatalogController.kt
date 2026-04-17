package com.sleekydz86.idolglow.productpackage.admin.ui

import com.sleekydz86.idolglow.productpackage.admin.application.AdminCatalogService
import com.sleekydz86.idolglow.productpackage.admin.application.dto.AdminReservationSlotResponse
import com.sleekydz86.idolglow.productpackage.admin.ui.request.CreateReservationSlotsRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "관리자 카탈로그", description = "관리자 상품, 옵션, 예약 슬롯 관리 API")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/admin")
class AdminCatalogController(
    private val adminCatalogService: AdminCatalogService,
) {

    @Operation(summary = "상품 예약 슬롯 조회", description = "특정 상품에 연결된 예약 슬롯 목록을 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "예약 슬롯 목록 조회 성공",
                content = [Content(array = ArraySchema(schema = Schema(implementation = AdminReservationSlotResponse::class)))]
            )
        ]
    )
    @GetMapping("/products/{productId}/slots")
    fun findSlots(
        @Parameter(description = "조회할 상품 ID", example = "1")
        @PathVariable productId: Long
    ): ResponseEntity<List<AdminReservationSlotResponse>> =
        ResponseEntity.ok(adminCatalogService.findSlots(productId))

    @Operation(summary = "예약 슬롯 일괄 생성", description = "지정한 기간과 시간 범위 기준으로 예약 슬롯을 일괄 생성합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "예약 슬롯 생성 성공",
                content = [Content(array = ArraySchema(schema = Schema(implementation = AdminReservationSlotResponse::class)))]
            )
        ]
    )
    @PostMapping("/products/{productId}/slots")
    fun createSlots(
        @Parameter(description = "예약 슬롯을 생성할 상품 ID", example = "1")
        @PathVariable productId: Long,
        @Valid @RequestBody request: CreateReservationSlotsRequest
    ): ResponseEntity<List<AdminReservationSlotResponse>> =
        ResponseEntity.ok(adminCatalogService.createSlots(productId, request))

    @Operation(summary = "예약 슬롯 삭제", description = "특정 예약 슬롯을 삭제합니다.")
    @ApiResponses(value = [ApiResponse(responseCode = "204", description = "예약 슬롯 삭제 성공")])
    @DeleteMapping("/slots/{slotId}")
    fun deleteSlot(@Parameter(description = "삭제할 예약 슬롯 ID", example = "10") @PathVariable slotId: Long): ResponseEntity<Void> {
        adminCatalogService.deleteSlot(slotId)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "상품 삭제", description = "특정 상품과 연결된 관리 대상 정보를 삭제합니다.")
    @ApiResponses(value = [ApiResponse(responseCode = "204", description = "상품 삭제 성공")])
    @DeleteMapping("/products/{productId}")
    fun deleteProduct(@Parameter(description = "삭제할 상품 ID", example = "1") @PathVariable productId: Long): ResponseEntity<Void> {
        adminCatalogService.deleteProduct(productId)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "옵션 삭제", description = "특정 옵션을 삭제합니다.")
    @ApiResponses(value = [ApiResponse(responseCode = "204", description = "옵션 삭제 성공")])
    @DeleteMapping("/options/{optionId}")
    fun deleteOption(@Parameter(description = "삭제할 옵션 ID", example = "2") @PathVariable optionId: Long): ResponseEntity<Void> {
        adminCatalogService.deleteOption(optionId)
        return ResponseEntity.noContent().build()
    }
}
