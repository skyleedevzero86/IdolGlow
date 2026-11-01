package com.sleekydz86.idolglow.survey.application

import com.sleekydz86.idolglow.survey.domain.SurveySubmission
import com.sleekydz86.idolglow.survey.domain.dto.SurveyFormStatisticsResponse
import com.sleekydz86.idolglow.survey.infrastructure.SurveyFormJpaRepository
import com.sleekydz86.idolglow.survey.infrastructure.SurveySubmissionJpaRepository
import com.sleekydz86.idolglow.user.auth.domain.UserOAuth
import com.sleekydz86.idolglow.user.auth.domain.vo.AuthProvider
import com.sleekydz86.idolglow.user.auth.infrastructure.UserOAuthJpaRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
@Transactional(readOnly = true)
class SurveyFormStatisticsService(
    private val surveyFormJpaRepository: SurveyFormJpaRepository,
    private val surveySubmissionJpaRepository: SurveySubmissionJpaRepository,
    private val userOAuthJpaRepository: UserOAuthJpaRepository,
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
            SurveyWrittenExportBuilder.from(form, submissions, writerIdentities(submissions)),
        )
    }

    private fun writerIdentities(submissions: List<SurveySubmission>): Map<Long, SurveyWriterIdentity> {
        val userIds = submissions.map { it.user.id }.distinct()
        if (userIds.isEmpty()) {
            return emptyMap()
        }
        val linked = userOAuthJpaRepository.findAllByUserIdIn(userIds).groupBy { it.userId }
        return submissions.associate { submission ->
            val user = submission.user
            val account = linkedAccount(linked[user.id].orEmpty())
            val name = account?.profileName?.takeIf { it.isNotBlank() } ?: user.nickname.value
            val access = account?.provider?.accessLabel() ?: "웹로그인"
            user.id to SurveyWriterIdentity(access, name, user.email)
        }
    }

    private fun linkedAccount(rows: List<UserOAuth>): UserOAuth? =
        LOGIN_PROVIDER_ORDER.firstNotNullOfOrNull { provider -> rows.firstOrNull { it.provider == provider } }
            ?: rows.firstOrNull { it.provider != AuthProvider.TEST }

    companion object {
        private val LOGIN_PROVIDER_ORDER = listOf(AuthProvider.GOOGLE, AuthProvider.KAKAO, AuthProvider.NAVER)
    }
}
