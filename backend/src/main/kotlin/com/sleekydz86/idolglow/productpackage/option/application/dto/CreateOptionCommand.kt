package com.sleekydz86.idolglow.productpackage.option.application.dto

import java.math.BigDecimal

data class CreateOptionCommand(
    val name: String,
    val description: String,
    val price: BigDecimal,
    val location: String,
)