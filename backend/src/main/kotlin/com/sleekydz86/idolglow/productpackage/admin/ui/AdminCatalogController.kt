package com.sleekydz86.idolglow.productpackage.admin.ui

import com.sleekydz86.idolglow.productpackage.admin.application.AdminCatalogService
import com.sleekydz86.idolglow.productpackage.admin.application.dto.AdminReservationSlotResponse
import com.sleekydz86.idolglow.productpackage.admin.ui.request.CreateReservationSlotsRequest
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

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/admin")
class AdminCatalogController(
    private val adminCatalogService: AdminCatalogService,
) {

    @GetMapping("/products/{productId}/slots")
    fun findSlots(
        @PathVariable productId: Long
    ): ResponseEntity<List<AdminReservationSlotResponse>> =
        ResponseEntity.ok(adminCatalogService.findSlots(productId))

    @PostMapping("/products/{productId}/slots")
    fun createSlots(
        @PathVariable productId: Long,
        @Valid @RequestBody request: CreateReservationSlotsRequest
    ): ResponseEntity<List<AdminReservationSlotResponse>> =
        ResponseEntity.ok(adminCatalogService.createSlots(productId, request))

    @DeleteMapping("/slots/{slotId}")
    fun deleteSlot(@PathVariable slotId: Long): ResponseEntity<Void> {
        adminCatalogService.deleteSlot(slotId)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/products/{productId}")
    fun deleteProduct(@PathVariable productId: Long): ResponseEntity<Void> {
        adminCatalogService.deleteProduct(productId)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/options/{optionId}")
    fun deleteOption(@PathVariable optionId: Long): ResponseEntity<Void> {
        adminCatalogService.deleteOption(optionId)
        return ResponseEntity.noContent().build()
    }
}
