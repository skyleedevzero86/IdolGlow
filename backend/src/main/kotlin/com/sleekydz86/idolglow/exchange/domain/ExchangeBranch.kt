package com.sleekydz86.idolglow.exchange.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(
    name = "exchange_branch",
    indexes = [
        Index(name = "idx_exchange_branch_currency", columnList = "currency"),
    ],
)
class ExchangeBranch(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(nullable = false, length = 200)
    val name: String,

    @Column(nullable = false, precision = 20, scale = 8)
    val rate: BigDecimal,

    @Column(nullable = false, length = 8)
    val currency: String,

    @Column(nullable = false)
    val lat: Double,

    @Column(nullable = false)
    val lng: Double,

    @Column(name = "sort_order", nullable = false)
    val sortOrder: Int = 0,

    @Column(name = "airport_hub", nullable = false)
    val airportHub: Boolean = false,
)
