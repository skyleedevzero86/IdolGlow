package com.sleekydz86.idolglow.payment.application.dto

import com.sleekydz86.idolglow.payment.domain.PaymentLog
import com.sleekydz86.idolglow.payment.domain.PaymentLogStep
import com.sleekydz86.idolglow.payment.domain.PaymentLogType
import java.time.LocalDateTime

data class AdminPaymentLogResponse(
    val logId: Long,
    val logType: PaymentLogType,
    val step: PaymentLogStep?,
    val requestUrl: String?,
    val httpMethod: String?,
    val httpStatus: Int?,
    val errorCode: String?,
    val errorMessage: String?,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun from(log: PaymentLog): AdminPaymentLogResponse =
            AdminPaymentLogResponse(
                logId = log.id,
                logType = log.logType,
                step = log.step,
                requestUrl = log.requestUrl,
                httpMethod = log.httpMethod,
                httpStatus = log.httpStatus,
                errorCode = log.errorCode,
                errorMessage = log.errorMessage,
                createdAt = log.createdAt,
            )
    }
}
