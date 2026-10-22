package com.sleekydz86.idolglow.payment.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "toss_billing_customers",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_toss_billing_customers_user_id", columnNames = ["user_id"]),
        UniqueConstraint(name = "uk_toss_billing_customers_customer_key", columnNames = ["customer_key"]),
    ],
)
class TossBillingCustomer(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(name = "customer_key", nullable = false, length = 36)
    val customerKey: String,
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
