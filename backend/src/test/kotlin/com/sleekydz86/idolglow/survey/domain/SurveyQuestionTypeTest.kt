package com.sleekydz86.idolglow.survey.domain

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("설문 문항 타입")
class SurveyQuestionTypeTest {
    @Test
    @DisplayName("단문형과 장문형은 직접 입력 문항이다")
    fun writtenTypesNeedText() {
        // given
        val written = listOf(SurveyQuestionType.SHORT_TEXT, SurveyQuestionType.LONG_TEXT, SurveyQuestionType.TEXT)
        val choices = listOf(SurveyQuestionType.SINGLE_CHOICE, SurveyQuestionType.MULTIPLE_CHOICE)

        // when
        val writtenFlags = written.map { it.isWrittenAnswer() }
        val choiceFlags = choices.map { it.isWrittenAnswer() }

        // then
        assertTrue(writtenFlags.all { it })
        assertFalse(choiceFlags.any { it })
    }
}
