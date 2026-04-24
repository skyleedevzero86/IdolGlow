package com.sleekydz86.idolglow.survey.application

import com.sleekydz86.idolglow.survey.domain.SurveyAnswer
import com.sleekydz86.idolglow.survey.domain.SurveyAnswerOption
import com.sleekydz86.idolglow.survey.domain.SurveyQuestionType
import com.sleekydz86.idolglow.survey.domain.SurveySubmission
import com.sleekydz86.idolglow.survey.domain.dto.SurveyFormResponse
import com.sleekydz86.idolglow.survey.domain.dto.SurveySubmissionResponse
import com.sleekydz86.idolglow.survey.infrastructure.SurveyFormJpaRepository
import com.sleekydz86.idolglow.survey.infrastructure.SurveySubmissionJpaRepository
import com.sleekydz86.idolglow.survey.ui.request.SubmitSurveyResponseRequest
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserSurveyFormService(
    private val surveyFormJpaRepository: SurveyFormJpaRepository,
    private val surveySubmissionJpaRepository: SurveySubmissionJpaRepository,
    private val userRepository: UserRepository,
) {
    @Transactional(readOnly = true)
    fun findCurrentForm(): SurveyFormResponse? =
        surveyFormJpaRepository.findFirstByActiveTrueOrderByIdDesc()?.let(SurveyFormResponse::from)

    fun submitCurrentForm(userId: Long, request: SubmitSurveyResponseRequest): SurveySubmissionResponse {
        val form = surveyFormJpaRepository.findFirstByActiveTrueOrderByIdDesc()
            ?: throw IllegalArgumentException("활성화된 설문지가 없습니다.")
        val user = userRepository.findById(userId)
            ?: throw IllegalArgumentException("ID가 $userId 인 사용자를 찾을 수 없습니다.")

        val answersByQuestionId = request.answers.associateBy { it.questionId }
        val submission = SurveySubmission(form = form, user = user)

        for (question in form.questions) {
            val answerRequest = answersByQuestionId[question.id]
            if (question.required && answerRequest == null) {
                throw IllegalArgumentException("필수 문항에 응답해 주세요. questionId=${question.id}")
            }
            if (answerRequest == null) {
                continue
            }
            val answer = SurveyAnswer(submission = submission, question = question)
            when (question.questionType) {
                SurveyQuestionType.TEXT -> {
                    val text = answerRequest.answerText?.trim().orEmpty()
                    if (question.required && text.isBlank()) {
                        throw IllegalArgumentException("필수 문항 텍스트 응답이 비어 있습니다. questionId=${question.id}")
                    }
                    answer.answerText = text.ifBlank { null }
                }
                SurveyQuestionType.SINGLE_CHOICE,
                SurveyQuestionType.MULTIPLE_CHOICE -> {
                    val selected = answerRequest.selectedOptions
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                    if (question.required && selected.isEmpty()) {
                        throw IllegalArgumentException("필수 객관식 문항 응답이 비어 있습니다. questionId=${question.id}")
                    }
                    if (question.questionType == SurveyQuestionType.SINGLE_CHOICE && selected.size > 1) {
                        throw IllegalArgumentException("단일 선택 문항은 하나만 선택할 수 있습니다. questionId=${question.id}")
                    }
                    val optionSet = question.options.map { it.optionText }.toSet()
                    for (option in selected) {
                        require(option in optionSet) { "문항 선택지에 없는 값을 제출했습니다. questionId=${question.id}" }
                    }
                    answer.selectedOptions.addAll(
                        selected.mapIndexed { idx, option ->
                            SurveyAnswerOption(
                                answer = answer,
                                displayOrder = idx + 1,
                                optionText = option,
                            )
                        },
                    )
                }
            }
            submission.answers.add(answer)
        }

        val saved = surveySubmissionJpaRepository.save(submission)
        return SurveySubmissionResponse.from(saved)
    }

    @Transactional(readOnly = true)
    fun findMyLatestSubmission(userId: Long): SurveySubmissionResponse? =
        surveySubmissionJpaRepository.findFirstByUserIdOrderByIdDesc(userId)?.let(SurveySubmissionResponse::from)
}
