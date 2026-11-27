package com.sleekydz86.idolglow.productpackage.product.domain

import java.math.BigDecimal
import java.time.LocalDate

fun Product.eventStatus(today: LocalDate): ProductEventStatus = eventSale?.status(today) ?: ProductEventStatus.NONE

fun Product.eventMinPrice(today: LocalDate): BigDecimal? = activeEventSale(today)?.price(basePrice, optionsMinPrice)

fun Product.eventTotalPrice(today: LocalDate): BigDecimal? = activeEventSale(today)?.price(basePrice, optionsTotalPrice)

fun Product.reservationStartPrice(today: LocalDate): BigDecimal = eventTotalPrice(today) ?: totalPrice

private fun Product.activeEventSale(today: LocalDate): ProductEventSale? =
    eventSale?.takeIf { it.status(today) == ProductEventStatus.ACTIVE }
