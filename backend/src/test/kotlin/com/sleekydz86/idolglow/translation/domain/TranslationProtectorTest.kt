package com.sleekydz86.idolglow.translation.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("번역 전 링크·마크다운 보호")
class TranslationProtectorTest {
    @Test
    fun linksImagesAndCodeAreHiddenAndRestored() {
        // given
        val markdown =
            "![포스터](/uploads/idolglow/a.png) 예매는 [여기](https://idolglow.test/book) 에서, " +
                "문의는 help@idolglow.test, 코드 `IG-2026` 입력"

        // when
        val protectedText = TranslationProtector.protect(markdown)
        val translated =
            protectedText.text
                .replace("예매는", "Book")
                .replace("에서", "")
                .replace("문의는", "contact")
                .replace("코드", "code")
                .replace("입력", "enter")
        val restored = TranslationProtector.restore(translated, protectedText.tokens)

        // then
        assertFalse(protectedText.text.contains("https://"))
        assertFalse(protectedText.text.contains("/uploads/"))
        assertFalse(protectedText.text.contains("@"))
        assertTrue(protectedText.text.contains("[여기]"))
        assertTrue(restored.contains("![포스터](/uploads/idolglow/a.png)"))
        assertTrue(restored.contains("](https://idolglow.test/book)"))
        assertTrue(restored.contains("help@idolglow.test"))
        assertTrue(restored.contains("`IG-2026`"))
    }

    @Test
    fun duplicatedOrInventedTokensAreReported() {
        // given
        val protectedText = TranslationProtector.protect("링크 https://idolglow.test 참고")

        // when
        val first = TranslationProtector.token(1)
        val second = TranslationProtector.token(2)
        val duplicated = TranslationProtector.tokenProblems("$first $first", protectedText.tokens.size)
        val invented = TranslationProtector.tokenProblems("$first $second", protectedText.tokens.size)

        // then
        assertEquals(listOf("token_1_count"), duplicated)
        assertEquals(listOf("token_2_unknown"), invented)
    }
}
