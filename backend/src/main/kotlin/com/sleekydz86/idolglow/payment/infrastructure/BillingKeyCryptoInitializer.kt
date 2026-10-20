package com.sleekydz86.idolglow.payment.infrastructure

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class BillingKeyCryptoInitializer(
    @param:Value("\${payment.toss.billing.crypto.secret:}")
    private val secret: String,
) {
    @PostConstruct
    fun init() {
        BillingKeyCrypto.configure(secret.trim())
    }
}
