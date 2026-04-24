package com.sleekydz86.idolglow.productpackage.admin.application

import com.sleekydz86.idolglow.productpackage.admin.infrastructure.AdminReservationQueryRepository
import com.sleekydz86.idolglow.productpackage.option.infrastructure.OptionJpaRepository
import com.sleekydz86.idolglow.productpackage.product.infrastructure.ProductJpaRepository
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationSlot
import com.sleekydz86.idolglow.productpackage.reservation.infrastructure.ReservationSlotJpaRepository
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.time.LocalDate

@Transactional(readOnly = true)
@Service
class AdminExportService(
    private val adminReservationQueryRepository: AdminReservationQueryRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val optionJpaRepository: OptionJpaRepository,
    private val reservationSlotJpaRepository: ReservationSlotJpaRepository,
) {

    fun exportReservationsCsv(visitDateFrom: LocalDate, visitDateTo: LocalDate): String {
        val rows = adminReservationQueryRepository.findReservationsByVisitDateRange(
            visitDateFrom,
            visitDateTo,
            MAX_ROWS
        )
        val lines = mutableListOf<String>()
        lines += joinRow(
            listOf(
                "reservationId",
                "userId",
                "productId",
                "productName",
                "visitDate",
                "status",
                "totalPrice",
                "cancelReason",
            )
        )
        for (r in rows) {
            val p = r.reservationSlot.product
            lines += joinRow(
                listOf(
                    r.id.toString(),
                    r.userId.toString(),
                    p.id.toString(),
                    csvEscape(p.name),
                    r.visitDate.toString(),
                    r.status.name,
                    r.totalPrice.toPlainString(),
                    r.cancelReason?.name ?: "",
                )
            )
        }
        return lines.joinToString("\r\n") + "\r\n"
    }

    fun exportPaymentsCsv(visitDateFrom: LocalDate, visitDateTo: LocalDate): String {
        val payments = adminReservationQueryRepository.findPaymentsByVisitDateRange(
            visitDateFrom,
            visitDateTo,
            MAX_ROWS
        )
        val lines = mutableListOf<String>()
        lines += joinRow(
            listOf(
                "paymentId",
                "paymentReference",
                "status",
                "amount",
                "reservationId",
                "productId",
                "productName",
                "visitDate",
                "failedAt",
                "failureReason",
            )
        )
        for (p in payments) {
            val r = p.reservation
            val pr = r.reservationSlot.product
            lines += joinRow(
                listOf(
                    p.id.toString(),
                    csvEscape(p.paymentReference),
                    p.status.name,
                    p.amount.toPlainString(),
                    r.id.toString(),
                    pr.id.toString(),
                    csvEscape(pr.name),
                    r.visitDate.toString(),
                    p.failedAt?.toString() ?: "",
                    csvEscape(p.failureReason ?: ""),
                )
            )
        }
        return lines.joinToString("\r\n") + "\r\n"
    }

    fun exportReservationsXlsx(visitDateFrom: LocalDate, visitDateTo: LocalDate): ByteArray {
        val rows = adminReservationQueryRepository.findReservationsByVisitDateRange(
            visitDateFrom,
            visitDateTo,
            MAX_ROWS
        )
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("reservations")
        val headerStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }
        val headers = listOf(
            "reservationId",
            "userId",
            "productId",
            "productName",
            "visitDate",
            "status",
            "totalPrice",
            "cancelReason",
        )
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, label ->
            headerRow.createCell(index).apply {
                setCellValue(label)
                cellStyle = headerStyle
            }
        }
        rows.forEachIndexed { rowIndex, reservation ->
            val row = sheet.createRow(rowIndex + 1)
            val product = reservation.reservationSlot.product
            row.createCell(0).setCellValue(reservation.id.toDouble())
            row.createCell(1).setCellValue(reservation.userId.toDouble())
            row.createCell(2).setCellValue(product.id.toDouble())
            row.createCell(3).setCellValue(product.name)
            row.createCell(4).setCellValue(reservation.visitDate.toString())
            row.createCell(5).setCellValue(reservation.status.name)
            row.createCell(6).setCellValue(reservation.totalPrice.toPlainString())
            row.createCell(7).setCellValue(reservation.cancelReason?.name ?: "")
        }
        headers.indices.forEach(sheet::autoSizeColumn)
        return ByteArrayOutputStream().use { out ->
            workbook.use { it.write(out) }
            out.toByteArray()
        }
    }

    fun exportPaymentsXlsx(visitDateFrom: LocalDate, visitDateTo: LocalDate): ByteArray {
        val payments = adminReservationQueryRepository.findPaymentsByVisitDateRange(
            visitDateFrom,
            visitDateTo,
            MAX_ROWS
        )
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("payments")
        val headerStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }
        val headers = listOf(
            "paymentId",
            "paymentReference",
            "status",
            "amount",
            "reservationId",
            "productId",
            "productName",
            "visitDate",
            "failedAt",
            "failureReason",
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
            val reservation = payment.reservation
            val product = reservation.reservationSlot.product
            row.createCell(0).setCellValue(payment.id.toDouble())
            row.createCell(1).setCellValue(payment.paymentReference)
            row.createCell(2).setCellValue(payment.status.name)
            row.createCell(3).setCellValue(payment.amount.toPlainString())
            row.createCell(4).setCellValue(reservation.id.toDouble())
            row.createCell(5).setCellValue(product.id.toDouble())
            row.createCell(6).setCellValue(product.name)
            row.createCell(7).setCellValue(reservation.visitDate.toString())
            row.createCell(8).setCellValue(payment.failedAt?.toString() ?: "")
            row.createCell(9).setCellValue(payment.failureReason ?: "")
        }
        headers.indices.forEach(sheet::autoSizeColumn)
        return ByteArrayOutputStream().use { out ->
            workbook.use { it.write(out) }
            out.toByteArray()
        }
    }

    fun exportProductsXlsx(): ByteArray {
        val products = productJpaRepository.findAll().sortedBy { it.id }
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("products")
        val headerStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }
        val headers = listOf(
            "productId",
            "name",
            "basePrice",
            "optionsTotalPrice",
            "minPrice",
            "totalPrice",
            "tagNames",
            "location",
            "slotCount",
            "description",
        )
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, label ->
            headerRow.createCell(index).apply {
                setCellValue(label)
                cellStyle = headerStyle
            }
        }
        products.forEachIndexed { rowIndex, product ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(product.id.toDouble())
            row.createCell(1).setCellValue(product.name)
            row.createCell(2).setCellValue(product.basePrice.toPlainString())
            row.createCell(3).setCellValue(product.optionsTotalPrice.toPlainString())
            row.createCell(4).setCellValue(product.minPrice.toPlainString())
            row.createCell(5).setCellValue(product.totalPrice.toPlainString())
            row.createCell(6).setCellValue(product.productTags.joinToString(",") { it.tagName })
            row.createCell(7).setCellValue(product.productLocation?.displayAddress() ?: "")
            row.createCell(8).setCellValue(product.reservationSlots.size.toDouble())
            row.createCell(9).setCellValue(product.description)
        }
        headers.indices.forEach(sheet::autoSizeColumn)
        return ByteArrayOutputStream().use { out ->
            workbook.use { it.write(out) }
            out.toByteArray()
        }
    }

    fun exportOptionsXlsx(): ByteArray {
        val options = optionJpaRepository.findAll().sortedBy { it.id }
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("options")
        val headerStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }
        val headers = listOf(
            "optionId",
            "name",
            "price",
            "location",
            "description",
        )
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, label ->
            headerRow.createCell(index).apply {
                setCellValue(label)
                cellStyle = headerStyle
            }
        }
        options.forEachIndexed { rowIndex, option ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(option.id.toDouble())
            row.createCell(1).setCellValue(option.name)
            row.createCell(2).setCellValue(option.price.toPlainString())
            row.createCell(3).setCellValue(option.location)
            row.createCell(4).setCellValue(option.description)
        }
        headers.indices.forEach(sheet::autoSizeColumn)
        return ByteArrayOutputStream().use { out ->
            workbook.use { it.write(out) }
            out.toByteArray()
        }
    }

    fun exportSlotsXlsx(productId: Long, dateFrom: LocalDate?, dateTo: LocalDate?): ByteArray {
        require(dateFrom == null || dateTo == null || !dateTo.isBefore(dateFrom)) {
            "dateTo는 dateFrom 이상이어야 합니다."
        }
        val slots = reservationSlotJpaRepository.findAllByProductIdOrderByReservationDateAscStartTimeAsc(productId)
            .asSequence()
            .filter { slot -> dateFrom == null || !slot.reservationDate.isBefore(dateFrom) }
            .filter { slot -> dateTo == null || !slot.reservationDate.isAfter(dateTo) }
            .toList()
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("slots")
        val headerStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }
        val headers = listOf(
            "slotId",
            "productId",
            "productName",
            "reservationDate",
            "startTime",
            "endTime",
            "status",
            "holdReservationId",
            "adminNote",
        )
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, label ->
            headerRow.createCell(index).apply {
                setCellValue(label)
                cellStyle = headerStyle
            }
        }
        slots.forEachIndexed { rowIndex, slot ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(slot.id.toDouble())
            row.createCell(1).setCellValue(slot.product.id.toDouble())
            row.createCell(2).setCellValue(slot.product.name)
            row.createCell(3).setCellValue(slot.reservationDate.toString())
            row.createCell(4).setCellValue(slot.startTime.toString())
            row.createCell(5).setCellValue(slot.endTime.toString())
            row.createCell(6).setCellValue(slotStatus(slot))
            row.createCell(7).setCellValue(slot.holdReservationId?.toString() ?: "")
            row.createCell(8).setCellValue(slot.adminNote ?: "")
        }
        headers.indices.forEach(sheet::autoSizeColumn)
        return ByteArrayOutputStream().use { out ->
            workbook.use { it.write(out) }
            out.toByteArray()
        }
    }

    private fun joinRow(fields: List<String>): String =
        fields.joinToString(",") { f ->
            if (f.contains(',') || f.contains('"') || f.contains('\n') || f.contains('\r')) {
                "\"${f.replace("\"", "\"\"")}\""
            } else {
                f
            }
        }

    private fun csvEscape(s: String): String =
        s.replace("\r\n", " ").replace("\n", " ").replace("\r", " ")

    companion object {
        private const val MAX_ROWS = 5000
    }

    private fun slotStatus(slot: ReservationSlot): String =
        when {
            slot.isStatusBooked -> "BOOKED"
            slot.holdReservationId != null -> "HOLD"
            else -> "AVAILABLE"
        }
}
