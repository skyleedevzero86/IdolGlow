package com.sleekydz86.idolglow.payment.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import com.sleekydz86.idolglow.payment.infrastructure.BillingKeyCrypto
import com.sleekydz86.idolglow.payment.infrastructure.BillingKeyCryptoConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "toss_billing_methods")
class TossBillingMethod(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(name = "customer_key", nullable = false, length = 100)
    val customerKey: String,
    @Convert(converter = BillingKeyCryptoConverter::class)
    @Column(name = "billing_key", nullable = false, length = 1000)
    var billingKey: String,
    @Column(name = "billing_key_hash", nullable = false, length = 64, unique = true)
    var billingKeyHash: String = BillingKeyCrypto.sha256Hex(billingKey),
    @Column(name = "card_company", length = 50)
    var cardCompany: String? = null,
    @Column(name = "card_number", length = 40)
    var cardNumber: String? = null,
    @Column(name = "card_type", length = 30)
    var cardType: String? = null,
    @Column(name = "owner_type", length = 30)
    var ownerType: String? = null,
    @Column(name = "authenticated_at")
    var authenticatedAt: LocalDateTime? = null,
    @Column(nullable = false)
    var active: Boolean = true,
) : BaseEntity() {
    fun replaceBillingKey(plain: String) {
        billingKey = plain
        billingKeyHash = BillingKeyCrypto.sha256Hex(plain)
    }

    fun deactivate() {
        active = false
    }
}
