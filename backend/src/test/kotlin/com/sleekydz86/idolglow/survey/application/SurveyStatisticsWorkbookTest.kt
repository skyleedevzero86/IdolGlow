package com.sleekydz86.idolglow.survey.application

import com.sleekydz86.idolglow.survey.domain.dto.SurveyFormStatisticsResponse
import com.sleekydz86.idolglow.survey.domain.dto.SurveyOptionStatisticsResponse
import com.sleekydz86.idolglow.survey.domain.dto.SurveyQuestionStatisticsResponse
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

@DisplayName("설문 통계 엑셀")
class SurveyStatisticsWorkbookTest {
    @Test
    @DisplayName("객관식 시트에는 선택 통계만 적는다")
    fun writesChoiceSheetWithoutWrittenRows() {
        // given
        val bytes = sampleWorkbook()

        // when
        XSSFWorkbook(ByteArrayInputStream(bytes)).use { workbook ->
            val choice = workbook.getSheet("객관식")

            // then
            assertEquals("취향 설문", choice.getRow(0).getCell(1).stringCellValue)
            assertEquals(2.0, choice.getRow(0).getCell(3).numericCellValue)
            assertEquals("선택지", choice.getRow(2).getCell(4).stringCellValue)
            assertEquals("시선을 사로잡는 나", choice.getRow(3).getCell(4).stringCellValue)
            assertEquals(50.0, choice.getRow(3).getCell(6).numericCellValue)
            assertEquals(null, choice.getRow(4))
        }
    }

    @Test
    @DisplayName("주관식 시트에 입력 건수와 아이디별 답변을 적는다")
    fun writesWrittenAnswersByUser() {
        // given
        val bytes = sampleWorkbook()

        // when
        XSSFWorkbook(ByteArrayInputStream(bytes)).use { workbook ->
            val subjective = workbook.getSheet("주관식")

            // then
            assertEquals("입력", subjective.getRow(2).getCell(3).stringCellValue)
            assertEquals("미입력", subjective.getRow(2).getCell(4).stringCellValue)
            assertEquals(1.0, subjective.getRow(3).getCell(3).numericCellValue)
            assertEquals(1.0, subjective.getRow(3).getCell(4).numericCellValue)
            assertEquals("아이디", subjective.getRow(6).getCell(0).stringCellValue)
            assertEquals(9.0, subjective.getRow(7).getCell(0).numericCellValue)
            assertEquals("강남에서 하루", subjective.getRow(7).getCell(4).stringCellValue)
            assertEquals("입력", subjective.getRow(7).getCell(5).stringCellValue)
            assertEquals("미입력", subjective.getRow(8).getCell(5).stringCellValue)
        }
    }

    private fun sampleWorkbook(): ByteArray = SurveyStatisticsWorkbook.write(sampleStatistics(), sampleWritten())

    private fun sampleStatistics(): SurveyFormStatisticsResponse =
        SurveyFormStatisticsResponse(
            formId = 3L,
            title = "취향 설문",
            participantCount = 2,
            questions =
                listOf(
                    choiceQuestion(),
                    SurveyQuestionStatisticsResponse(
                        questionId = 12L,
                        order = 2,
                        title = "하루 이야기",
                        required = false,
                        written = true,
                        responseCount = 1,
                        options = emptyList(),
                    ),
                ),
        )

    private fun choiceQuestion(): SurveyQuestionStatisticsResponse =
        SurveyQuestionStatisticsResponse(
            questionId = 11L,
            order = 1,
            title = "만나고 싶은 나",
            required = true,
            written = false,
            responseCount = 2,
            options =
                listOf(
                    SurveyOptionStatisticsResponse(
                        label = "시선을 사로잡는 나 — 걸크러시",
                        count = 1,
                        percent = 50,
                    ),
                ),
        )

    private fun sampleWritten(): SurveyWrittenExport =
        SurveyWrittenExport(
            title = "취향 설문",
            participantCount = 2,
            summaries =
                listOf(
                    SurveyWrittenQuestionSummary(
                        order = 2,
                        title = "하루 이야기",
                        typeLabel = "장문형",
                        enteredCount = 1,
                        emptyCount = 1,
                    ),
                ),
            rows =
                listOf(
                    writtenRow(9L, "강남에서 하루"),
                    writtenRow(15L, ""),
                ),
        )

    private fun writtenRow(
        userId: Long,
        answer: String,
    ): SurveyWrittenAnswerRow =
        SurveyWrittenAnswerRow(
            userId = userId,
            order = 2,
            typeLabel = "장문형",
            questionTitle = "하루 이야기",
            answer = answer,
        )
}
