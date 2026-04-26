package com.sleekydz86.idolglow.exchange.domain

import com.sleekydz86.idolglow.user.user.domain.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(
    name = "exchange_alert",
    indexes = [
        Index(name = "idx_exchange_alert_user", columnList = "user_id"),
    ],
)
class ExchangeAlert(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "from_currency", nullable = false, length = 8)
    val fromCurrency: String,

    @Column(name = "to_currency", nullable = false, length = 8)
    val toCurrency: String,

    @Column(name = "target_rate", nullable = false, precision = 20, scale = 8)
    val targetRate: BigDecimal,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)
