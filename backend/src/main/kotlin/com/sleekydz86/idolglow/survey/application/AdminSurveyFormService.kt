package com.sleekydz86.idolglow.survey.application

import com.sleekydz86.idolglow.survey.domain.SurveyForm
import com.sleekydz86.idolglow.survey.domain.SurveyQuestion
import com.sleekydz86.idolglow.survey.domain.SurveyQuestionOption
import com.sleekydz86.idolglow.survey.domain.SurveyQuestionType
import com.sleekydz86.idolglow.survey.domain.dto.SurveyFormResponse
import com.sleekydz86.idolglow.survey.infrastructure.SurveyFormJpaRepository
import com.sleekydz86.idolglow.survey.ui.request.AdminUpsertSurveyFormRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AdminSurveyFormService(
    private val surveyFormJpaRepository: SurveyFormJpaRepository,
) {
    companion object {
        private const val MAX_SURVEY_QUESTIONS = 5
    }

    @Transactional(readOnly = true)
    fun findCurrent(): SurveyFormResponse? =
        surveyFormJpaRepository.findFirstByActiveTrueOrderByIdDesc()?.let(SurveyFormResponse::from)

    fun upsertCurrent(request: AdminUpsertSurveyFormRequest): SurveyFormResponse {
        require(request.questions.isNotEmpty()) { "문항이 없으면 저장할 수 없습니다." }
        require(request.questions.size <= MAX_SURVEY_QUESTIONS) {
            "문항은 최대 ${MAX_SURVEY_QUESTIONS}개까지 등록할 수 있습니다."
        }
        val form = surveyFormJpaRepository.findFirstByActiveTrueOrderByIdDesc()
            ?.apply {
                title = request.title.trim()
                description = request.description?.trim()?.takeIf { it.isNotBlank() }
            }
            ?: SurveyForm(
                title = request.title.trim(),
                description = request.description?.trim()?.takeIf { it.isNotBlank() },
                active = true,
            )

        val nextQuestions = request.questions
            .sortedBy { it.order }
            .map { q ->
                validateQuestion(q.type, q.options)
                val question = SurveyQuestion(
                    form = form,
                    displayOrder = q.order,
                    title = q.title.trim(),
                    description = q.description?.trim()?.takeIf { it.isNotBlank() },
                    questionType = q.type,
                    required = q.required,
                )
                question.options.addAll(
                    q.options
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .mapIndexed { idx, option ->
                            SurveyQuestionOption(
                                question = question,
                                displayOrder = idx + 1,
                                optionText = option,
                            )
                        },
                )
                question
            }

        form.replaceQuestions(nextQuestions)
        val saved = surveyFormJpaRepository.save(form)
        return SurveyFormResponse.from(saved)
    }

    private fun validateQuestion(type: SurveyQuestionType, options: List<String>) {
        if (type == SurveyQuestionType.TEXT) {
            return
        }
        val normalized = options.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        require(normalized.isNotEmpty()) { "객관식 문항은 선택지 1개 이상이 필요합니다." }
    }
}
