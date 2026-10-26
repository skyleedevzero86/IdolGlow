package com.sleekydz86.idolglow.survey.domain.dto

import com.sleekydz86.idolglow.survey.domain.SurveyAnswer
import com.sleekydz86.idolglow.survey.domain.SurveyAnswerOption
import com.sleekydz86.idolglow.survey.domain.SurveyForm
import com.sleekydz86.idolglow.survey.domain.SurveyQuestion
import com.sleekydz86.idolglow.survey.domain.SurveyQuestionOption
import com.sleekydz86.idolglow.survey.domain.SurveyQuestionType
import com.sleekydz86.idolglow.survey.domain.SurveySubmission
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.vo.Nickname
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("설문 문항 통계")
class SurveyFormStatisticsResponseTest {
    @Test
    @DisplayName("선택형 문항은 응답 비율을 내고 주관식은 막대를 만들지 않는다")
    fun summarizesChoiceRatioAndSkipsWrittenBars() {
        // given
        val form = SurveyForm(id = 1L, title = "취향 설문")
        val choice = choiceQuestion(form)
        val written = writtenQuestion(form)
        form.questions.addAll(listOf(choice, written))
        val first = submission(form, choice, "걸크러시", writtenText = "시크")
        val second = submission(form, choice, "드리미", writtenText = null)

        // when
        val statistics = SurveyFormStatisticsResponse.from(form, listOf(first, second))

        // then
        assertEquals(2, statistics.participantCount)
        val choiceStat = statistics.questions.first { it.questionId == choice.id }
        assertEquals(50, choiceStat.options.first { it.label == "걸크러시" }.percent)
        assertEquals(1, choiceStat.options.first { it.label == "걸크러시" }.count)
        val writtenStat = statistics.questions.first { it.questionId == written.id }
        assertTrue(writtenStat.written)
        assertTrue(writtenStat.options.isEmpty())
        assertEquals(1, writtenStat.responseCount)
    }

    private fun choiceQuestion(form: SurveyForm): SurveyQuestion {
        val question =
            SurveyQuestion(
                id = 11L,
                form = form,
                displayOrder = 1,
                title = "만나고 싶은 나",
                questionType = SurveyQuestionType.SINGLE_CHOICE,
                required = true,
            )
        question.options.add(SurveyQuestionOption(question = question, displayOrder = 1, optionText = "걸크러시"))
        question.options.add(SurveyQuestionOption(question = question, displayOrder = 2, optionText = "드리미"))
        return question
    }

    private fun writtenQuestion(form: SurveyForm): SurveyQuestion =
        SurveyQuestion(
            id = 12L,
            form = form,
            displayOrder = 2,
            title = "키워드",
            questionType = SurveyQuestionType.SHORT_TEXT,
            required = false,
        )

    private fun submission(
        form: SurveyForm,
        choice: SurveyQuestion,
        selected: String,
        writtenText: String?,
    ): SurveySubmission {
        val user = User(email = "$selected@example.com", nickname = Nickname.of("팬하나"))
        val submission = SurveySubmission(form = form, user = user)
        val choiceAnswer = SurveyAnswer(submission = submission, question = choice)
        choiceAnswer.selectedOptions.add(
            SurveyAnswerOption(answer = choiceAnswer, displayOrder = 1, optionText = selected),
        )
        submission.answers.add(choiceAnswer)
        if (writtenText != null) {
            val written = form.questions.first { it.questionType.isWrittenAnswer() }
            val textAnswer = SurveyAnswer(submission = submission, question = written)
            textAnswer.answerText = writtenText
            submission.answers.add(textAnswer)
        }
        return submission
    }
}
