package com.sleekydz86.idolglow.translation.application

import com.sleekydz86.idolglow.translation.domain.TranslationGlossaryTerm
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

@DisplayName("번역 지시문과 응답 읽기")
class TranslationPromptTest {
    private val objectMapper = JsonMapper.builder().build()

    @Test
    fun jsonIsReadEvenWithSurroundingText() {
        // given
        val raw = "Here you go:\n```json\n{\"title\": \"Notice\", \"body\": \"Opening ⟦1⟧\"}\n```"

        // when
        val fields = TranslationPrompt.parseFields(raw, objectMapper)

        // then
        assertEquals(mapOf("title" to "Notice", "body" to "Opening ⟦1⟧"), fields)
    }

    @Test
    fun brokenJsonGivesNothing() {
        // given
        val raw = "Sorry, translation failed"

        // when
        val fields = TranslationPrompt.parseFields(raw, objectMapper)

        // then
        assertNull(fields)
    }

    @Test
    fun judgeScoreOutsideRangeIsIgnored() {
        // given
        val good = "{\"score\": 4, \"issues\": []}"
        val bad = "{\"score\": 9}"

        // when
        val goodScore = TranslationPrompt.parseScore(good, objectMapper)
        val badScore = TranslationPrompt.parseScore(bad, objectMapper)

        // then
        assertEquals(4, goodScore)
        assertNull(badScore)
    }

    @Test
    fun glossaryForTheLanguageIsInThePrompt() {
        // given
        val glossary =
            listOf(
                TranslationGlossaryTerm.fixed("세종문화회관", "ja", "世宗文化会館"),
                TranslationGlossaryTerm.fixed("세종문화회관", "en", "Sejong Center"),
                TranslationGlossaryTerm.keep("IdolGlow"),
            )

        // when
        val prompt = TranslationPrompt.system("ja", glossary)

        // then
        assertTrue(prompt.contains("Japanese"))
        assertTrue(prompt.contains("\"세종문화회관\" -> \"世宗文化会館\""))
        assertTrue(prompt.contains("keep as \"IdolGlow\""))
        assertTrue(!prompt.contains("Sejong Center"))
    }
}
