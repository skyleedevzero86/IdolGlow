package com.sleekydz86.idolglow.persistence

import com.sleekydz86.idolglow.review.domain.ReviewRating
import com.sleekydz86.idolglow.user.user.domain.vo.Nickname
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("JPA 임베디드 생성자")
class JpaEmbeddableConstructorTest {
    @DisplayName("닉네임 임베디드 타입은 기본 생성자를 제공한다")
    @Test
    fun nicknameEmbeddableHasDefaultConstructor() {

        // given

        // when
        // then
        assertDoesNotThrow {
            Nickname::class.java.getDeclaredConstructor()
        }
    }

    @DisplayName("리뷰 평점 임베디드 타입은 기본 생성자를 제공한다")
    @Test
    fun reviewRatingEmbeddableHasDefaultConstructor() {

        // given

        // when
        // then
        assertDoesNotThrow {
            ReviewRating::class.java.getDeclaredConstructor()
        }
    }
}
