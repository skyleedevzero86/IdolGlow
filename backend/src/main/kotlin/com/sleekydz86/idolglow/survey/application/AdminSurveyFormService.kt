package com.sleekydz86.idolglow.survey.application

import com.sleekydz86.idolglow.survey.domain.SurveyForm
import com.sleekydz86.idolglow.survey.domain.SurveyFormPrimaryCategory
import com.sleekydz86.idolglow.survey.domain.SurveyFormSecondaryCategory
import com.sleekydz86.idolglow.survey.domain.SurveyFormStatus
import com.sleekydz86.idolglow.survey.domain.SurveyQuestion
import com.sleekydz86.idolglow.survey.domain.SurveyQuestionOption
import com.sleekydz86.idolglow.survey.domain.SurveyQuestionType
import com.sleekydz86.idolglow.survey.domain.dto.SurveyFormPageResponse
import com.sleekydz86.idolglow.survey.domain.dto.SurveyFormResponse
import com.sleekydz86.idolglow.survey.domain.dto.SurveyFormSummaryResponse
import com.sleekydz86.idolglow.survey.infrastructure.SurveyFormJpaRepository
import com.sleekydz86.idolglow.survey.ui.request.AdminUpsertSurveyFormRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
@Transactional
class AdminSurveyFormService(
    private val surveyFormJpaRepository: SurveyFormJpaRepository,
) {
    companion object {
        private const val MAX_SURVEY_QUESTIONS = 5
        private const val DEFAULT_PAGE_SIZE = 10
        private const val MAX_PAGE_SIZE = 50
    }

    @Transactional(readOnly = true)
    fun findCurrent(): SurveyFormResponse? = surveyFormJpaRepository.findFirstByActiveTrueOrderByIdDesc()?.let(SurveyFormResponse::from)

    @Transactional(readOnly = true)
    fun list(
        keyword: String?,
        status: SurveyFormStatus?,
        primaryCategory: SurveyFormPrimaryCategory?,
        secondaryCategory: SurveyFormSecondaryCategory?,
    ): List<SurveyFormSummaryResponse> {
        val normalized = keyword?.trim().orEmpty()
        val normalizedFilters = normalizeFilters(primaryCategory, secondaryCategory)
        val forms =
            surveyFormJpaRepository.search(
                keyword = normalized.takeIf { it.isNotBlank() },
                status = status,
                primaryCategory = normalizedFilters.first,
                secondaryCategory = normalizedFilters.second,
            )
        return forms.map(SurveyFormSummaryResponse::from)
    }

    @Transactional(readOnly = true)
    fun listPage(
        keyword: String?,
        status: SurveyFormStatus?,
        primaryCategory: SurveyFormPrimaryCategory?,
        secondaryCategory: SurveyFormSecondaryCategory?,
        page: Int?,
        size: Int?,
    ): SurveyFormPageResponse {
        val normalizedKeyword = keyword?.trim().orEmpty()
        val normalizedFilters = normalizeFilters(primaryCategory, secondaryCategory)
        val resolvedSize = (size ?: DEFAULT_PAGE_SIZE).coerceIn(1, MAX_PAGE_SIZE)
        val requestedPage = (page ?: 1).coerceAtLeast(1)
        val firstResult =
            fetchPage(
                keyword = normalizedKeyword,
                status = status,
                primaryCategory = normalizedFilters.first,
                secondaryCategory = normalizedFilters.second,
                pageIndex = requestedPage - 1,
                size = resolvedSize,
            )
        val result =
            if (firstResult.totalElements > 0 && requestedPage > firstResult.totalPages) {
                fetchPage(
                    keyword = normalizedKeyword,
                    status = status,
                    primaryCategory = normalizedFilters.first,
                    secondaryCategory = normalizedFilters.second,
                    pageIndex = firstResult.totalPages - 1,
                    size = resolvedSize,
                )
            } else {
                firstResult
            }

        return SurveyFormPageResponse.from(result)
    }

    @Transactional(readOnly = true)
    fun find(id: Long): SurveyFormResponse = SurveyFormResponse.from(findForm(id))

    fun create(request: AdminUpsertSurveyFormRequest): SurveyFormResponse =
        saveForm(
            form =
                SurveyForm(
                    title = request.title.trim(),
                    active = true,
                ),
            request = request,
        )

    fun update(
        id: Long,
        request: AdminUpsertSurveyFormRequest,
    ): SurveyFormResponse =
        saveForm(
            form =
                findForm(id).apply {
                    title = request.title.trim()
                },
            request = request,
        )

    fun upsertCurrent(request: AdminUpsertSurveyFormRequest): SurveyFormResponse {
        val form =
            surveyFormJpaRepository
                .findFirstByActiveTrueOrderByIdDesc()
                ?.apply {
                    title = request.title.trim()
                }
                ?: SurveyForm(
                    title = request.title.trim(),
                    active = true,
                )
        return saveForm(form, request)
    }

    private fun saveForm(
        form: SurveyForm,
        request: AdminUpsertSurveyFormRequest,
    ): SurveyFormResponse {
        require(request.questions.isNotEmpty()) { "문항이 없으면 저장할 수 없습니다." }
        require(request.questions.size <= MAX_SURVEY_QUESTIONS) {
            "문항은 최대 ${MAX_SURVEY_QUESTIONS}개까지 등록할 수 있습니다."
        }
        val secondaryCategory = validateCategory(request.primaryCategory, request.secondaryCategory)
        form.status = request.status
        form.primaryCategory = request.primaryCategory
        form.secondaryCategory = secondaryCategory
        form.replaceDescription(request.description, request.descriptionTags)

        val nextQuestions =
            request.questions
                .sortedBy { it.order }
                .map { q ->
                    validateQuestion(q.type, q.options)
                    val question =
                        SurveyQuestion(
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

    private fun findForm(id: Long): SurveyForm =
        surveyFormJpaRepository
            .findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "설문지를 찾을 수 없습니다.") }

    private fun fetchPage(
        keyword: String,
        status: SurveyFormStatus?,
        primaryCategory: SurveyFormPrimaryCategory?,
        secondaryCategory: SurveyFormSecondaryCategory?,
        pageIndex: Int,
        size: Int,
    ): Page<SurveyForm> {
        val pageable = PageRequest.of(pageIndex.coerceAtLeast(0), size)
        return surveyFormJpaRepository.search(
            keyword = keyword.takeIf { it.isNotBlank() },
            status = status,
            primaryCategory = primaryCategory,
            secondaryCategory = secondaryCategory,
            pageable = pageable,
        )
    }

    private fun validateCategory(
        primaryCategory: SurveyFormPrimaryCategory,
        secondaryCategory: SurveyFormSecondaryCategory?,
    ): SurveyFormSecondaryCategory? {
        if (primaryCategory == SurveyFormPrimaryCategory.ALL) {
            require(secondaryCategory == null) { "전체 카테고리는 소분류를 선택할 수 없습니다." }
            return null
        }

        val secondary = requireNotNull(secondaryCategory) { "${primaryCategory.label}의 소분류를 선택해 주세요." }
        require(secondary.primaryCategory == primaryCategory) { "대분류와 소분류가 일치하지 않습니다." }
        return secondary
    }

    private fun normalizeFilters(
        primaryCategory: SurveyFormPrimaryCategory?,
        secondaryCategory: SurveyFormSecondaryCategory?,
    ): Pair<SurveyFormPrimaryCategory?, SurveyFormSecondaryCategory?> {
        val normalizedPrimary = primaryCategory?.takeUnless { it == SurveyFormPrimaryCategory.ALL }
        require(secondaryCategory == null || normalizedPrimary == null || secondaryCategory.primaryCategory == normalizedPrimary) {
            "대분류와 소분류가 일치하지 않습니다."
        }
        return normalizedPrimary to secondaryCategory
    }

    private fun validateQuestion(
        type: SurveyQuestionType,
        options: List<String>,
    ) {
        if (type == SurveyQuestionType.TEXT) {
            return
        }
        val normalized = options.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        require(normalized.isNotEmpty()) { "객관식 문항은 선택지 1개 이상이 필요합니다." }
    }
}
