package com.sleekydz86.idolglow.payment.infrastructure

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BillingKeyCryptoTest {
    @BeforeEach
    fun setUp() {
        BillingKeyCrypto.configure("test-billing-crypto-secret")
    }

    @Test
    fun `암호화 후 복호화하면 평문이 된다`() {
        val plain = "Z_t5vOvQxrj4499PeiJcjen28-V2RyqgYTwN44Rdzk0="
        val encrypted = BillingKeyCrypto.encrypt(plain)
        assertTrue(encrypted.startsWith("v1:"))
        assertNotEquals(plain, encrypted)
        assertEquals(plain, BillingKeyCrypto.decrypt(encrypted))
    }

    @Test
    fun `같은 평문은 같은 해시를 갖는다`() {
        val plain = "same-billing-key"
        assertEquals(BillingKeyCrypto.sha256Hex(plain), BillingKeyCrypto.sha256Hex(plain))
    }
}
