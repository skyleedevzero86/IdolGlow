package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentLog
import com.sleekydz86.idolglow.payment.domain.PaymentLogStep
import com.sleekydz86.idolglow.payment.domain.PaymentLogType
import com.sleekydz86.idolglow.payment.infrastructure.PaymentLogJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Transactional(propagation = Propagation.REQUIRED)
@Service
class PaymentLogCommandService(
    private val paymentLogJpaRepository: PaymentLogJpaRepository,
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
    ): PaymentLog =
        paymentLogJpaRepository.save(
            PaymentLog(
                payment = payment,
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
        )
}
