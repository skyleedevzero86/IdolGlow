package com.sleekydz86.idolglow.survey.application

import com.sleekydz86.idolglow.survey.domain.dto.SurveyFormStatisticsResponse
import com.sleekydz86.idolglow.survey.infrastructure.SurveyFormJpaRepository
import com.sleekydz86.idolglow.survey.infrastructure.SurveySubmissionJpaRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
@Transactional(readOnly = true)
class SurveyFormStatisticsService(
    private val surveyFormJpaRepository: SurveyFormJpaRepository,
    private val surveySubmissionJpaRepository: SurveySubmissionJpaRepository,
) {
    fun statistics(id: Long): SurveyFormStatisticsResponse {
        val form =
            surveyFormJpaRepository
                .findById(id)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "설문지를 찾을 수 없습니다.") }
        val submissions = surveySubmissionJpaRepository.findAllByFormId(id)
        return SurveyFormStatisticsResponse.from(form, submissions)
    }

    fun exportXlsx(id: Long): ByteArray {
        val form =
            surveyFormJpaRepository
                .findById(id)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "설문지를 찾을 수 없습니다.") }
        val submissions = surveySubmissionJpaRepository.findAllByFormId(id)
        return SurveyStatisticsWorkbook.write(
            SurveyFormStatisticsResponse.from(form, submissions),
            SurveyWrittenExportBuilder.from(form, submissions),
        )
    }
}
