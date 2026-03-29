package com.sleekydz86.idolglow.productpackage.product.ui

import com.sleekydz86.idolglow.productpackage.product.application.ProductCommandService
import com.sleekydz86.idolglow.productpackage.product.application.ProductQueryService
import com.sleekydz86.idolglow.productpackage.product.application.dto.ProductCreatedResponse
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductPagingQueryResponse
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
        @RequestParam(required = false, defaultValue = "20") size: Int?,
        @RequestParam(required = false) tag: String?,
    ): ResponseEntity<List<ProductPagingQueryResponse>> {
        val resolvedSize = (size ?: 20).coerceIn(1, 50)
        val resolvedTag = tag?.trim()
            ?.takeIf { it.isNotEmpty() }

        val result = productQueryService.findProductsByNoOffset(
            lastId = lastId,
            size = resolvedSize,
            tagName = resolvedTag
        )
        return ResponseEntity.ok(result)
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
