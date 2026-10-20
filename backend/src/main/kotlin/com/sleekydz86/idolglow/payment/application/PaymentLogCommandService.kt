package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentLog
import com.sleekydz86.idolglow.payment.domain.PaymentLogStep
import com.sleekydz86.idolglow.payment.domain.PaymentLogType
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Service
class PaymentLogCommandService(
    private val paymentLogTxService: PaymentLogTxService,
) {
    fun append(
        payment: Payment?,
        orderId: String?,
        paymentKey: String?,
        logType: PaymentLogType,
        step: PaymentLogStep? = null,
        requestUrl: String? = null,
        httpMethod: String? = null,
        httpStatus: Int? = null,
        requestBody: String? = null,
        responseBody: String? = null,
        errorCode: String? = null,
        errorMessage: String? = null,
        stackTrace: String? = null,
        traceId: String? = null,
    ): PaymentLog {
        val paymentId = payment?.id?.takeIf { it > 0L }
        val log =
            PaymentLog(
                payment = null,
                orderId = orderId,
                paymentKey = paymentKey,
                logType = logType,
                step = step,
                requestUrl = requestUrl,
                httpMethod = httpMethod,
                httpStatus = httpStatus,
                requestBody = requestBody,
                responseBody = responseBody,
                errorCode = errorCode,
                errorMessage = errorMessage,
                stackTrace = stackTrace,
                traceId = traceId,
            )
        runAfterCompletion { paymentLogTxService.saveDetached(paymentId, log) }
        return log
    }

    private fun runAfterCompletion(action: () -> Unit) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action()
            return
        }
        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCompletion(status: Int) = action()
            },
        )
    }
}
