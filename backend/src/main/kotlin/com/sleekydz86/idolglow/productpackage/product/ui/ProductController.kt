package com.sleekydz86.idolglow.productpackage.product.ui

import com.sleekydz86.idolglow.productpackage.product.application.ProductCommandService
import com.sleekydz86.idolglow.productpackage.product.application.ProductQueryService
import com.sleekydz86.idolglow.productpackage.product.application.dto.ProductCreatedResponse
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductBrowseResult
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductSpecificResponse
import com.sleekydz86.idolglow.productpackage.product.ui.request.CreateProductRequest
import com.sleekydz86.idolglow.productpackage.product.ui.request.toCommand
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RequestMapping("/products")
@RestController
class ProductController(
    private val productQueryService: ProductQueryService,
    private val productCommandService: ProductCommandService
) : ProductApi {

    @GetMapping
    override fun findProducts(
        @RequestParam(required = false) lastId: Long?,
        @RequestParam(required = false) offset: Int?,
        @RequestParam(required = false, defaultValue = "20") size: Int?,
        @RequestParam(required = false) tag: String?,
        @RequestParam(required = false) tags: List<String>?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) minPrice: String?,
        @RequestParam(required = false) maxPrice: String?,
        @RequestParam(required = false) visitDate: String?,
        @RequestParam(required = false) reservableOnly: Boolean?,
        @RequestParam(required = false) sort: String?,
        @RequestParam(required = false) nearLatitude: String?,
        @RequestParam(required = false) nearLongitude: String?,
        @RequestParam(required = false) radiusMeters: Int?,
    ): ResponseEntity<ProductBrowseResult> {
        val resolvedSize = (size ?: 20).coerceIn(1, 50)
        val params = ProductBrowseRequestParser.parse(
            lastId = lastId,
            offset = offset,
            size = resolvedSize,
            tag = tag,
            tags = tags,
            keyword = keyword,
            minPrice = minPrice,
            maxPrice = maxPrice,
            visitDate = visitDate,
            reservableOnly = reservableOnly,
            sort = sort,
            nearLatitude = nearLatitude,
            nearLongitude = nearLongitude,
            radiusMeters = radiusMeters,
        )
        return ResponseEntity.ok(productQueryService.browseProducts(params))
    }

    @GetMapping("/{productId}")
    override fun findProduct(@PathVariable productId: Long): ResponseEntity<ProductSpecificResponse> =
        ResponseEntity.ok(productQueryService.findProductSpecificById(productId))

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    override fun createProduct(@Valid @RequestBody request: CreateProductRequest): ResponseEntity<ProductCreatedResponse> {
        val product = productCommandService.createProduct(request.toCommand())
        return ResponseEntity.created(URI.create("/products/" + product.id))
            .body(ProductCreatedResponse(id = product.id))
    }
}
