package com.sleekydz86.idolglow.productpackage.admin.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

data class ProductConversionRow(
    val productId: Long,
    val productName: String,
    val booked: Long,
    val canceled: Long,
    @field:Schema(description = "확정 대비 확정 취소 합에서 확정이 차지하는 비율")
    val conversionRate: BigDecimal?,
)
