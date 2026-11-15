package com.sleekydz86.idolglow.translation.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("번역 기계 검사")
class TranslationGuardTest {
    @Test
    fun cleanTranslationPasses() {
        // given
        val input =
            input("en", "서울 야경 투어, 가격 55,000원, 19:30 출발", "Seoul night tour, price 55,000 KRW, departs at 19:30")

        // when
        val result = TranslationGuard.check(input)

        // then
        assertTrue(result.passed())
    }

    @Test
    fun changedPriceIsHardFailure() {
        // given
        val input = input("en", "서울 야경 투어, 가격 55,000원", "Seoul night tour, price 5,500 KRW")

        // when
        val result = TranslationGuard.check(input)

        // then
        assertTrue(result.hard.contains("number:text:55,000"))
    }

    @Test
    fun brokenPlaceholderIsHardFailure() {
        // given
        val protectedText = TranslationProtector.protect("자세한 내용은 [여기](https://idolglow.test/notice) 를 보세요")
        val input =
            GuardInput(
                locale = "en",
                source = mapOf("text" to protectedText.text),
                translated = mapOf("text" to "See details here"),
                tokenCounts = mapOf("text" to protectedText.tokens.size),
                glossary = emptyList(),
            )

        // when
        val result = TranslationGuard.check(input)

        // then
        assertTrue(result.hard.contains("token_1_count:text"))
    }

    @Test
    fun untranslatedKoreanIsHardFailure() {
        // given
        val input = input("ja", "남산타워와 한강 야경을 함께 보는 코스", "남산타워와 한강 야경을 함께 보는 코스")

        // when
        val result = TranslationGuard.check(input)

        // then
        assertTrue(result.hard.contains("untranslated:text"))
        assertTrue(result.hard.contains("wrong_language:text"))
    }

    @Test
    fun refusalIsHardFailure() {
        // given
        val input = input("en", "남산타워 야경 코스", "I'm sorry, I cannot translate this.")

        // when
        val result = TranslationGuard.check(input)

        // then
        assertTrue(result.hard.contains("refusal:text"))
    }

    @Test
    fun missingAndUnknownFieldsAreHardFailures() {
        // given
        val input =
            GuardInput(
                locale = "en",
                source = mapOf("title" to "공지", "body" to "본문"),
                translated = mapOf("title" to "Notice", "summary" to "Body"),
                tokenCounts = emptyMap(),
                glossary = emptyList(),
            )

        // when
        val result = TranslationGuard.check(input)

        // then
        assertTrue(result.hard.contains("missing_field:body"))
        assertTrue(result.hard.contains("unknown_field:summary"))
    }

    @Test
    fun glossaryViolationGoesToReviewOnly() {
        // given
        val glossary =
            listOf(
                TranslationGlossaryTerm.fixed("세종문화회관", "en", "Sejong Center for the Performing Arts"),
                TranslationGlossaryTerm.keep("IdolGlow"),
            )
        val input =
            GuardInput(
                locale = "en",
                source = mapOf("text" to "IdolGlow 가 세종문화회관 공연을 소개합니다"),
                translated = mapOf("text" to "Idol Glow introduces a show at Sejong Cultural Center"),
                tokenCounts = emptyMap(),
                glossary = glossary,
            )

        // when
        val result = TranslationGuard.check(input)

        // then
        assertTrue(result.hard.isEmpty())
        assertEquals(setOf("glossary:text:세종문화회관", "glossary:text:IdolGlow"), result.soft.toSet())
    }

    @Test
    fun keptKoreanBrandDoesNotCountAsUntranslated() {
        // given
        val input =
            GuardInput(
                locale = "en",
                source = mapOf("text" to "아이돌글로우 회원 혜택 안내"),
                translated = mapOf("text" to "아이돌글로우 member benefits guide"),
                tokenCounts = emptyMap(),
                glossary = listOf(TranslationGlossaryTerm.keep("아이돌글로우")),
            )

        // when
        val result = TranslationGuard.check(input)

        // then
        assertTrue(result.passed())
    }

    @Test
    fun suspiciousLengthGoesToReviewOnly() {
        // given
        val input = input("en", "남산타워와 한강 야경을 함께 보는 두 시간짜리 야간 산책 코스입니다", "Night walk")

        // when
        val result = TranslationGuard.check(input)

        // then
        assertTrue(result.hard.isEmpty())
        assertTrue(result.soft.contains("length_ratio:text"))
    }

    private fun input(
        locale: String,
        source: String,
        translated: String,
    ): GuardInput =
        GuardInput(
            locale = locale,
            source = mapOf("text" to source),
            translated = mapOf("text" to translated),
            tokenCounts = mapOf("text" to 0),
            glossary = emptyList(),
        )
}
