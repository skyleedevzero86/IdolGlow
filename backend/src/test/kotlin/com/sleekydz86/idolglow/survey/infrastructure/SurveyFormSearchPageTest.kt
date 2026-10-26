package com.sleekydz86.idolglow.survey.infrastructure

import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.survey.domain.SurveyForm
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional

@Transactional
@DisplayName("설문지 목록 검색")
class SurveyFormSearchPageTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var surveyFormJpaRepository: SurveyFormJpaRepository

    @Test
    @DisplayName("검색어 없이도 설문 페이지 조회가 된다")
    fun returnsPageWhenKeywordIsAbsent() {
        // given
        val pageable = PageRequest.of(0, 10)

        // when
        val page = surveyFormJpaRepository.searchWithoutKeyword(null, null, null, pageable)

        // then
        assertTrue(page.content.size <= pageable.pageSize)
    }

    @Test
    @DisplayName("설명 본문 검색어로 해당 설문지만 페이지에 포함한다")
    fun findsFormByDescriptionKeyword() {
        // given
        val form = SurveyForm(title = "설명검색표식")
        form.replaceDescription("바닷가 설문 본문", emptyList())
        surveyFormJpaRepository.saveAndFlush(form)
        val pageable = PageRequest.of(0, 10)

        // when
        val page = surveyFormJpaRepository.searchByKeyword("바닷가", null, null, null, pageable)

        // then
        assertEquals(1, page.totalElements)
    }
}
