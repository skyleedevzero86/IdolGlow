package com.sleekydz86.idolglow.survey.application

import com.sleekydz86.idolglow.survey.domain.dto.SurveyFormStatisticsResponse
import com.sleekydz86.idolglow.survey.domain.dto.SurveyQuestionStatisticsResponse
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.ByteArrayOutputStream

object SurveyStatisticsWorkbook {
    private const val SUMMARY_ROW = 0
    private const val HEADER_ROW = 2
    private const val DATA_START_ROW = 3
    private const val RESPONSE_COUNT_COLUMN = 3
    private const val CHOICE_COLUMN = 4
    private const val COUNT_COLUMN = 5
    private const val PERCENT_COLUMN = 6
    private const val LABEL_SEPARATOR = " — "
    private const val COLUMN_WIDTH = 18 * 256
    private val HEADERS = listOf("번호", "문항", "필수", "응답수", "선택지", "선택수", "비율")

    fun write(
        statistics: SurveyFormStatisticsResponse,
        written: SurveyWrittenExport,
    ): ByteArray {
        val workbook = XSSFWorkbook()
        return try {
            val sheet = workbook.createSheet("객관식")
            writeSummary(sheet, statistics)
            writeHeader(workbook, sheet)
            writeQuestions(sheet, statistics.questions.filter { !it.written })
            sizeColumns(sheet)
            SurveyWrittenAnswersSheet.write(workbook, written)
            ByteArrayOutputStream().use { output ->
                workbook.write(output)
                output.toByteArray()
            }
        } finally {
            workbook.close()
        }
    }

    private fun writeSummary(
        sheet: Sheet,
        statistics: SurveyFormStatisticsResponse,
    ) {
        val row = sheet.createRow(SUMMARY_ROW)
        row.createCell(0).setCellValue("설문")
        row.createCell(1).setCellValue(statistics.title)
        row.createCell(2).setCellValue("참여자")
        row.createCell(RESPONSE_COUNT_COLUMN).setCellValue(statistics.participantCount.toDouble())
    }

    private fun writeHeader(
        workbook: XSSFWorkbook,
        sheet: Sheet,
    ) {
        val headerStyle =
            workbook.createCellStyle().apply {
                fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
            }
        val row = sheet.createRow(HEADER_ROW)
        HEADERS.forEachIndexed { index, label ->
            row.createCell(index).apply {
                setCellValue(label)
                cellStyle = headerStyle
            }
        }
    }

    private fun writeQuestions(
        sheet: Sheet,
        questions: List<SurveyQuestionStatisticsResponse>,
    ) {
        var rowIndex = DATA_START_ROW
        for (question in questions) {
            rowIndex = writeQuestion(sheet, question, rowIndex)
        }
    }

    private fun writeQuestion(
        sheet: Sheet,
        question: SurveyQuestionStatisticsResponse,
        startRow: Int,
    ): Int {
        question.options.forEachIndexed { index, option ->
            writeChoiceRow(sheet, question, option.label, option.count, option.percent, startRow + index)
        }
        return startRow + question.options.size
    }

    private fun writeChoiceRow(
        sheet: Sheet,
        question: SurveyQuestionStatisticsResponse,
        label: String,
        count: Int,
        percent: Int,
        rowIndex: Int,
    ) {
        val row = sheet.createRow(rowIndex)
        fillQuestionCells(row, question)
        row.createCell(CHOICE_COLUMN).setCellValue(visibleLabel(label))
        row.createCell(COUNT_COLUMN).setCellValue(count.toDouble())
        row.createCell(PERCENT_COLUMN).setCellValue(percent.toDouble())
    }

    private fun fillQuestionCells(
        row: org.apache.poi.ss.usermodel.Row,
        question: SurveyQuestionStatisticsResponse,
    ) {
        row.createCell(0).setCellValue(question.order.toDouble())
        row.createCell(1).setCellValue(question.title)
        row.createCell(2).setCellValue(if (question.required) "필수" else "선택")
        row.createCell(RESPONSE_COUNT_COLUMN).setCellValue(question.responseCount.toDouble())
    }

    private fun visibleLabel(option: String): String {
        val index = option.lastIndexOf(LABEL_SEPARATOR)
        return if (index > 0) option.substring(0, index) else option
    }

    private fun sizeColumns(sheet: Sheet) {
        HEADERS.indices.forEach { index -> sheet.setColumnWidth(index, COLUMN_WIDTH) }
    }
}
