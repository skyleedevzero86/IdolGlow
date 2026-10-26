package com.sleekydz86.idolglow.survey.application

import com.sleekydz86.idolglow.survey.domain.SurveyForm
import com.sleekydz86.idolglow.survey.domain.SurveyQuestion
import com.sleekydz86.idolglow.survey.domain.SurveyQuestionType
import com.sleekydz86.idolglow.survey.domain.SurveySubmission

data class SurveyWrittenExport(
    val title: String,
    val participantCount: Int,
    val summaries: List<SurveyWrittenQuestionSummary>,
    val rows: List<SurveyWrittenAnswerRow>,
)

data class SurveyWrittenQuestionSummary(
    val order: Int,
    val title: String,
    val typeLabel: String,
    val enteredCount: Int,
    val emptyCount: Int,
)

data class SurveyWrittenAnswerRow(
    val userId: Long,
    val order: Int,
    val typeLabel: String,
    val questionTitle: String,
    val answer: String,
)

object SurveyWrittenExportBuilder {
    fun from(
        form: SurveyForm,
        submissions: List<SurveySubmission>,
    ): SurveyWrittenExport {
        val questions = writtenQuestions(form)
        return SurveyWrittenExport(
            title = form.title,
            participantCount = submissions.size,
            summaries = questions.map { summary(it, submissions) },
            rows = answerRows(questions, submissions),
        )
    }

    private fun writtenQuestions(form: SurveyForm): List<SurveyQuestion> =
        form.questions.filter { it.questionType.isWrittenAnswer() }.sortedBy { it.displayOrder }

    private fun summary(
        question: SurveyQuestion,
        submissions: List<SurveySubmission>,
    ): SurveyWrittenQuestionSummary {
        val entered = submissions.count { answerText(it, question.id).isNotBlank() }
        return SurveyWrittenQuestionSummary(
            order = question.displayOrder,
            title = question.title,
            typeLabel = writtenTypeLabel(question.questionType),
            enteredCount = entered,
            emptyCount = submissions.size - entered,
        )
    }

    private fun answerRows(
        questions: List<SurveyQuestion>,
        submissions: List<SurveySubmission>,
    ): List<SurveyWrittenAnswerRow> =
        submissions.sortedBy { it.user.id }.flatMap { submission ->
            questions.map { question -> answerRow(submission, question) }
        }

    private fun answerRow(
        submission: SurveySubmission,
        question: SurveyQuestion,
    ): SurveyWrittenAnswerRow =
        SurveyWrittenAnswerRow(
            userId = submission.user.id,
            order = question.displayOrder,
            typeLabel = writtenTypeLabel(question.questionType),
            questionTitle = question.title,
            answer = answerText(submission, question.id),
        )

    private fun answerText(
        submission: SurveySubmission,
        questionId: Long,
    ): String =
        submission.answers
            .firstOrNull { it.question.id == questionId }
            ?.answerText
            ?.trim()
            .orEmpty()

    private fun writtenTypeLabel(type: SurveyQuestionType): String =
        if (type == SurveyQuestionType.SHORT_TEXT) "단답형" else "장문형"
}
