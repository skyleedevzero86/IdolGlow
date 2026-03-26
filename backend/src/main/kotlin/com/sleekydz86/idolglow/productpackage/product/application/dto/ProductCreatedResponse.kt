package com.sleekydz86.idolglow.productpackage.product.application.dto

import io.swagger.v3.oas.annotations.media.Schema

data class ProductCreatedResponse(
    @field:Schema(description = "생성된 상품 ID", example = "1")
    val id: Long,
)
