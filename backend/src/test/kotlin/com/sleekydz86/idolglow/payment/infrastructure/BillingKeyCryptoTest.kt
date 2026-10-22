package com.sleekydz86.idolglow.payment.infrastructure

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("빌링키 암호화")
class BillingKeyCryptoTest {
    @BeforeEach
    fun setUp() {
        BillingKeyCrypto.configure("test-billing-crypto-secret")
    }

    @DisplayName("암호화 후 복호화하면 평문이 된다")
    @Test
    fun decryptsToOriginalPlaintext() {
        // given
        val plain = "Z_t5vOvQxrj4499PeiJcjen28-V2RyqgYTwN44Rdzk0="
        // when
        val encrypted = BillingKeyCrypto.encrypt(plain)
        // then
        assertTrue(encrypted.startsWith("v1:"))
        assertNotEquals(plain, encrypted)
        assertEquals(plain, BillingKeyCrypto.decrypt(encrypted))
    }

    @DisplayName("같은 평문은 같은 해시를 갖는다")
    @Test
    fun samePlaintextSharesHash() {
        // given
        // when
        val plain = "same-billing-key"
        // then
        assertEquals(BillingKeyCrypto.sha256Hex(plain), BillingKeyCrypto.sha256Hex(plain))
    }
}

