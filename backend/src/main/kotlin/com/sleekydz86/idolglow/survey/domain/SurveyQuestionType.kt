package com.sleekydz86.idolglow.survey.domain

enum class SurveyQuestionType {
    SHORT_TEXT,
    LONG_TEXT,
    SINGLE_CHOICE,
    MULTIPLE_CHOICE,
    TEXT,
    ;

    fun isWrittenAnswer(): Boolean = this == SHORT_TEXT || this == LONG_TEXT || this == TEXT
}
