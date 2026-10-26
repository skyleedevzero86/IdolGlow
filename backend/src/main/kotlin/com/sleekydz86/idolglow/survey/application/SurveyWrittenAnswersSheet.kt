package com.sleekydz86.idolglow.survey.application

import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook

object SurveyWrittenAnswersSheet {
    private const val SUMMARY_ROW = 0
    private const val SUMMARY_HEADER_ROW = 2
    private const val SUMMARY_DATA_ROW = 3
    private const val DETAIL_GAP = 2
    private const val PARTICIPANT_COLUMN = 3
    private const val ENTERED_COLUMN = 3
    private const val EMPTY_COLUMN = 4
    private const val QUESTION_COLUMN = 3
    private const val ANSWER_COLUMN = 4
    private const val STATUS_COLUMN = 5
    private const val WIDTH_UNIT = 256
    private const val USER_WIDTH = 14 * WIDTH_UNIT
    private const val ORDER_WIDTH = 10 * WIDTH_UNIT
    private const val TYPE_WIDTH = 12 * WIDTH_UNIT
    private const val QUESTION_WIDTH = 28 * WIDTH_UNIT
    private const val ANSWER_WIDTH = 48 * WIDTH_UNIT
    private const val STATUS_WIDTH = 12 * WIDTH_UNIT
    private val SUMMARY_HEADERS = listOf("번호", "문항", "유형", "입력", "미입력")
    private val DETAIL_HEADERS = listOf("아이디", "번호", "유형", "질문", "답변", "입력여부")

    fun write(
        workbook: XSSFWorkbook,
        export: SurveyWrittenExport,
    ) {
        val sheet = workbook.createSheet("주관식")
        val headerStyle = headerStyle(workbook)
        writeSurveyLine(sheet, export)
        writeSummary(sheet, export, headerStyle)
        val detailHeaderRow = SUMMARY_DATA_ROW + export.summaries.size + DETAIL_GAP
        writeDetailHeader(sheet, detailHeaderRow, headerStyle)
        writeDetails(sheet, export.rows, detailHeaderRow + 1)
        sizeColumns(sheet)
    }

    private fun writeSurveyLine(
        sheet: Sheet,
        export: SurveyWrittenExport,
    ) {
        val row = sheet.createRow(SUMMARY_ROW)
        row.createCell(0).setCellValue("설문")
        row.createCell(1).setCellValue(export.title)
        row.createCell(2).setCellValue("참여자")
        row.createCell(PARTICIPANT_COLUMN).setCellValue(export.participantCount.toDouble())
    }

    private fun writeSummary(
        sheet: Sheet,
        export: SurveyWrittenExport,
        headerStyle: CellStyle,
    ) {
        val header = sheet.createRow(SUMMARY_HEADER_ROW)
        SUMMARY_HEADERS.forEachIndexed { index, label ->
            header.createCell(index).apply {
                setCellValue(label)
                cellStyle = headerStyle
            }
        }
        if (export.summaries.isEmpty()) {
            sheet.createRow(SUMMARY_DATA_ROW).createCell(0).setCellValue("주관식 문항이 없습니다.")
            return
        }
        export.summaries.forEachIndexed { index, item -> writeSummaryRow(sheet, item, SUMMARY_DATA_ROW + index) }
    }

    private fun writeSummaryRow(
        sheet: Sheet,
        item: SurveyWrittenQuestionSummary,
        rowIndex: Int,
    ) {
        val row = sheet.createRow(rowIndex)
        row.createCell(0).setCellValue(item.order.toDouble())
        row.createCell(1).setCellValue(item.title)
        row.createCell(2).setCellValue(item.typeLabel)
        row.createCell(ENTERED_COLUMN).setCellValue(item.enteredCount.toDouble())
        row.createCell(EMPTY_COLUMN).setCellValue(item.emptyCount.toDouble())
    }

    private fun writeDetailHeader(
        sheet: Sheet,
        rowIndex: Int,
        headerStyle: CellStyle,
    ) {
        val row = sheet.createRow(rowIndex)
        DETAIL_HEADERS.forEachIndexed { index, label ->
            row.createCell(index).apply {
                setCellValue(label)
                cellStyle = headerStyle
            }
        }
    }

    private fun writeDetails(
        sheet: Sheet,
        rows: List<SurveyWrittenAnswerRow>,
        startRow: Int,
    ) {
        rows.forEachIndexed { index, item -> writeDetailRow(sheet, item, startRow + index) }
    }

    private fun writeDetailRow(
        sheet: Sheet,
        item: SurveyWrittenAnswerRow,
        rowIndex: Int,
    ) {
        val row = sheet.createRow(rowIndex)
        row.createCell(0).setCellValue(item.userId.toDouble())
        row.createCell(1).setCellValue(item.order.toDouble())
        row.createCell(2).setCellValue(item.typeLabel)
        row.createCell(QUESTION_COLUMN).setCellValue(item.questionTitle)
        row.createCell(ANSWER_COLUMN).setCellValue(item.answer)
        row.createCell(STATUS_COLUMN).setCellValue(if (item.answer.isBlank()) "미입력" else "입력")
    }

    private fun headerStyle(workbook: XSSFWorkbook): CellStyle =
        workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }

    private fun sizeColumns(sheet: Sheet) {
        val widths = listOf(USER_WIDTH, ORDER_WIDTH, TYPE_WIDTH, QUESTION_WIDTH, ANSWER_WIDTH, STATUS_WIDTH)
        widths.forEachIndexed { index, width -> sheet.setColumnWidth(index, width) }
    }
}
