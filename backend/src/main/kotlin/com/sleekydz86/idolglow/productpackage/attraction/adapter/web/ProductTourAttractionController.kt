package com.sleekydz86.idolglow.productpackage.attraction.ui

import com.sleekydz86.idolglow.productpackage.attraction.application.ProductTourAttractionQueryService
import com.sleekydz86.idolglow.productpackage.attraction.domain.dto.ProductTourAttractionResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/products")
class ProductTourAttractionController(
    private val productTourAttractionQueryService: ProductTourAttractionQueryService,
) : ProductTourAttractionApi {
    @GetMapping(value = ["/{productId}/tour-attractions", "/{productId}/tourist-attractions"])
    override fun findTourAttractions(
        @PathVariable productId: Long,
        @RequestParam(required = false, defaultValue = "10") size: Int?,
        @RequestParam(required = false, name = "base_ym") baseYm: String?,
        @RequestParam(required = false) areaCode: Int?,
        @RequestParam(required = false) signguCode: Int?,
        @RequestParam(required = false) category: String?,
    ): ResponseEntity<ProductTourAttractionResponse> {
        return ResponseEntity.ok(
            productTourAttractionQueryService.findAttractionsByProduct(
                productId = productId,
                size = size ?: 10,
                baseYm = baseYm,
                category = category,
                areaCode = areaCode,
                signguCode = signguCode,
            )
        )
    }
}
