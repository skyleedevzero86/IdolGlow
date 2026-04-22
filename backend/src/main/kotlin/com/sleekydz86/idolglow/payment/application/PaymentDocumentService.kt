package com.sleekydz86.idolglow.payment.application

import com.lowagie.text.Document
import com.lowagie.text.Font
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.Phrase
import com.lowagie.text.Rectangle
import com.lowagie.text.pdf.BaseFont
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter
import com.sleekydz86.idolglow.payment.domain.Payment
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.format.DateTimeFormatter

@Service
class PaymentDocumentService {

    fun exportPaymentsXlsx(payments: List<Payment>): ByteArray {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("payments")
        val headerStyle = workbook.createCellStyle().apply {
            fillForegroundColor = org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.index
            fillPattern = org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND
        }

        val headers = listOf(
            "결제ID",
            "예약ID",
            "회원ID",
            "상품명",
            "결제상태",
            "결제수단",
            "결제참조",
            "주문번호",
            "결제금액",
            "취소금액",
            "실패사유",
            "결제일시",
            "실패일시",
            "취소일시",
            "방문일",
            "방문시작",
            "방문종료",
        )

        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, label ->
            headerRow.createCell(index).apply {
                setCellValue(label)
                cellStyle = headerStyle
            }
        }

        payments.forEachIndexed { rowIndex, payment ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(payment.id.toDouble())
            row.createCell(1).setCellValue(payment.reservation.id.toDouble())
            row.createCell(2).setCellValue(payment.reservation.userId.toDouble())
            row.createCell(3).setCellValue(payment.reservation.reservationSlot.product.name)
            row.createCell(4).setCellValue(payment.status.name)
            row.createCell(5).setCellValue(payment.provider.name)
            row.createCell(6).setCellValue(payment.paymentReference)
            row.createCell(7).setCellValue(payment.orderId)
            row.createCell(8).setCellValue(payment.amount.toPlainString())
            row.createCell(9).setCellValue(payment.cancelAmount.toPlainString())
            row.createCell(10).setCellValue(payment.failureReason ?: "")
            row.createCell(11).setCellValue(payment.approvedAt?.format(DATE_TIME_FORMATTER) ?: "")
            row.createCell(12).setCellValue(payment.failedAt?.format(DATE_TIME_FORMATTER) ?: "")
            row.createCell(13).setCellValue(payment.canceledAt?.format(DATE_TIME_FORMATTER) ?: "")
            row.createCell(14).setCellValue(payment.reservation.visitDate.toString())
            row.createCell(15).setCellValue(payment.reservation.visitStartTime.toString())
            row.createCell(16).setCellValue(payment.reservation.visitEndTime.toString())
        }

        headers.indices.forEach(sheet::autoSizeColumn)

        return ByteArrayOutputStream().use { out ->
            workbook.use { it.write(out) }
            out.toByteArray()
        }
    }

    fun buildReceiptPdf(payment: Payment, receiverLabel: String): ByteArray {
        val baseFont = loadBaseFont()
        val titleFont = Font(baseFont, 18f, Font.BOLD)
        val sectionFont = Font(baseFont, 12f, Font.BOLD)
        val bodyFont = Font(baseFont, 11f, Font.NORMAL)
        val mutedFont = Font(baseFont, 10f, Font.NORMAL)

        val document = Document(PageSize.A4, 36f, 36f, 40f, 40f)
        return ByteArrayOutputStream().use { out ->
            PdfWriter.getInstance(document, out)
            document.open()
            document.add(Paragraph("IdolGlow 결제 영수증", titleFont))
            document.add(Paragraph("발급 대상: $receiverLabel", mutedFont))
            document.add(Paragraph(" "))
            document.add(Paragraph(receiptTitle(payment), sectionFont))
            document.add(Paragraph(" "))
            document.add(infoTable(payment, bodyFont, sectionFont))
            document.add(Paragraph(" "))
            document.add(Paragraph("본 영수증은 시스템에서 발급한 결제 확인 문서입니다.", mutedFont))
            document.close()
            out.toByteArray()
        }
    }

    private fun infoTable(payment: Payment, bodyFont: Font, sectionFont: Font): PdfPTable {
        val table = PdfPTable(floatArrayOf(1.4f, 3.6f)).apply {
            widthPercentage = 100f
            setSpacingBefore(8f)
        }

        fun addRow(label: String, value: String) {
            table.addCell(cell(label, sectionFont))
            table.addCell(cell(value, bodyFont))
        }

        addRow("결제번호", payment.id.toString())
        addRow("결제참조", payment.paymentReference)
        addRow("주문번호", payment.orderId)
        addRow("상품명", payment.reservation.reservationSlot.product.name)
        addRow("결제상태", payment.status.name)
        addRow("결제금액", "${payment.amount.toPlainString()} KRW")
        addRow("취소금액", "${payment.cancelAmount.toPlainString()} KRW")
        addRow("결제수단", payment.gatewayMethod ?: payment.provider.name)
        addRow("방문일시", "${payment.reservation.visitDate} ${payment.reservation.visitStartTime} ~ ${payment.reservation.visitEndTime}")
        addRow("승인일시", payment.approvedAt?.format(DATE_TIME_FORMATTER) ?: "-")
        addRow("취소일시", payment.canceledAt?.format(DATE_TIME_FORMATTER) ?: "-")
        addRow("실패사유", payment.failureReason ?: "-")
        return table
    }

    private fun cell(value: String, font: Font): PdfPCell =
        PdfPCell(Phrase(value, font)).apply {
            setPadding(8f)
            border = Rectangle.BOX
        }

    private fun receiptTitle(payment: Payment): String =
        when (payment.status) {
            com.sleekydz86.idolglow.payment.domain.PaymentStatus.SUCCEEDED -> "결제 성공 확인"
            com.sleekydz86.idolglow.payment.domain.PaymentStatus.CANCELED,
            com.sleekydz86.idolglow.payment.domain.PaymentStatus.REFUNDED,
            com.sleekydz86.idolglow.payment.domain.PaymentStatus.PARTIAL_CANCELED,
            -> "결제 취소 확인"
            else -> "결제 확인"
        }

    private fun loadBaseFont(): BaseFont {
        val candidate = FONT_CANDIDATES.firstOrNull { File(it).exists() }
        return if (candidate != null) {
            BaseFont.createFont(candidate, BaseFont.IDENTITY_H, BaseFont.EMBEDDED)
        } else {
            BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED)
        }
    }

    companion object {
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        private val FONT_CANDIDATES = listOf(
            "C:/Windows/Fonts/malgun.ttf",
            "C:/Windows/Fonts/NanumGothic.ttf",
            "/usr/share/fonts/truetype/nanum/NanumGothic.ttf",
        )
    }
}
