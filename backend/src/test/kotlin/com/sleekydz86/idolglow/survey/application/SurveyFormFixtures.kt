package com.sleekydz86.idolglow.survey.application

import com.sleekydz86.idolglow.survey.domain.RecommendationSignalType
import com.sleekydz86.idolglow.survey.domain.SurveyAnswer
import com.sleekydz86.idolglow.survey.domain.SurveyAnswerOption
import com.sleekydz86.idolglow.survey.domain.SurveyForm
import com.sleekydz86.idolglow.survey.domain.SurveyQuestion
import com.sleekydz86.idolglow.survey.domain.SurveyQuestionType
import com.sleekydz86.idolglow.survey.domain.SurveySubmission
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.vo.Nickname

internal class SurveyFormFixtures {
    private val form = SurveyForm(id = 1L, title = "취향 설문")
    private val user = User(id = 1L, email = "fan@example.com", nickname = Nickname.of("팬하나"))
    private val submission = SurveySubmission(id = 10L, form = form, user = user)
    private var nextQuestionId = 100L

    fun choice(
        signal: RecommendationSignalType?,
        vararg selected: String,
    ): SurveyFormFixtures {
        val question = question(signal, SurveyQuestionType.MULTIPLE_CHOICE)
        val answer = SurveyAnswer(submission = submission, question = question)
        selected.forEachIndexed { index, text ->
            answer.selectedOptions.add(SurveyAnswerOption(answer = answer, displayOrder = index + 1, optionText = text))
        }
        submission.answers.add(answer)
        return this
    }

    fun text(
        signal: RecommendationSignalType?,
        text: String,
    ): SurveyFormFixtures {
        val answer = SurveyAnswer(submission = submission, question = question(signal, SurveyQuestionType.SHORT_TEXT))
        answer.answerText = text
        submission.answers.add(answer)
        return this
    }

    fun build(): SurveySubmission = submission

    private fun question(
        signal: RecommendationSignalType?,
        type: SurveyQuestionType,
    ): SurveyQuestion {
        val question =
            SurveyQuestion(
                id = nextQuestionId++,
                form = form,
                displayOrder = form.questions.size + 1,
                title = "문항",
                questionType = type,
                recommendationSignal = signal,
            )
        form.questions.add(question)
        return question
    }
}
