package com.sleekydz86.idolglow.productpackage.admin.application

import com.sleekydz86.idolglow.productpackage.admin.infrastructure.AdminReservationQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Transactional(readOnly = true)
@Service
class AdminExportService(
    private val adminReservationQueryRepository: AdminReservationQueryRepository,
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
}
