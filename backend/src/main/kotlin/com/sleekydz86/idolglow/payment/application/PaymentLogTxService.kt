package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.domain.PaymentLog
import com.sleekydz86.idolglow.payment.infrastructure.PaymentJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.PaymentLogJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class PaymentLogTxService(
    private val paymentLogJpaRepository: PaymentLogJpaRepository,
    private val paymentJpaRepository: PaymentJpaRepository,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun saveDetached(
        paymentId: Long?,
        log: PaymentLog,
    ): PaymentLog {
        val payment = paymentId?.let { paymentJpaRepository.findById(it).orElse(null) }
        return paymentLogJpaRepository.save(
            PaymentLog(
                payment = payment,
                orderId = log.orderId,
                paymentKey = log.paymentKey,
                logType = log.logType,
                step = log.step,
                requestUrl = log.requestUrl,
                httpMethod = log.httpMethod,
                httpStatus = log.httpStatus,
                requestBody = log.requestBody,
                responseBody = log.responseBody,
                errorCode = log.errorCode,
                errorMessage = log.errorMessage,
                stackTrace = log.stackTrace,
                traceId = log.traceId,
            ),
        )
    }
}
