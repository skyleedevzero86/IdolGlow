package com.sleekydz86.idolglow.persistence

import com.sleekydz86.idolglow.review.domain.ReviewRating
import com.sleekydz86.idolglow.user.user.domain.vo.Nickname
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test

class JpaEmbeddableConstructorTest {
    @Test
    fun `닉네임_임베디드_타입은_기본_생성자를_제공한다`() {
        // given

        // when
        // then
        assertDoesNotThrow {
            Nickname::class.java.getDeclaredConstructor()
        }
    }

    @Test
    fun `리뷰_평점_임베디드_타입은_기본_생성자를_제공한다`() {
        // given

        // when
        // then
        assertDoesNotThrow {
            ReviewRating::class.java.getDeclaredConstructor()
        }
    }
}
