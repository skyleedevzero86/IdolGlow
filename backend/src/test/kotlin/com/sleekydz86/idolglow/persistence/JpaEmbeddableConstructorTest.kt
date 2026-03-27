package com.sleekydz86.idolglow.persistence

import com.sleekydz86.idolglow.review.domain.ReviewRating
import com.sleekydz86.idolglow.user.user.domain.vo.Nickname
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test

class JpaEmbeddableConstructorTest {

    @Test
    fun `nickname embeddable exposes no arg constructor for hibernate`() {
        assertDoesNotThrow {
            Nickname::class.java.getDeclaredConstructor()
        }
    }

    @Test
    fun `review rating embeddable exposes no arg constructor for hibernate`() {
        assertDoesNotThrow {
            ReviewRating::class.java.getDeclaredConstructor()
        }
    }
}
