package com.sleekydz86.idolglow.payment.application

import tools.jackson.databind.JsonNode
import com.sleekydz86.idolglow.payment.domain.Payment
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

object TossPaymentResponseMapper {

    fun applyConfirmSuccess(payment: Payment, json: JsonNode, rawJson: String) {
        payment.paymentKey = json.path("paymentKey").asText(null)
        payment.externalStatus = json.path("status").asText(null)
        payment.orderName = json.path("orderName").asText(null)
        payment.currency = json.path("currency").asText(null)
        payment.gatewayMethod = json.path("method").asText(null)
        payment.gatewayType = json.path("type").asText(null)
        payment.lastTransactionKey = json.path("lastTransactionKey").asText(null)

        json.path("suppliedAmount").takeIf { it.isNumber }?.let { payment.suppliedAmount = it.decimalValue() }
        json.path("vat").takeIf { it.isNumber }?.let { payment.vat = it.decimalValue() }
        json.path("taxFreeAmount").takeIf { it.isNumber }?.let { payment.taxFreeAmount = it.decimalValue() }

        payment.requestedAt = parseDate(json.path("requestedAt").asText(null))
        payment.approvedAt = parseDate(json.path("approvedAt").asText(null))

        val card = json.path("card")
        if (!card.isMissingNode && !card.isNull) {
            payment.cardCompany = card.path("company").asText(null)
            payment.cardNumber = card.path("number").asText(null)
            if (card.hasNonNull("installmentPlanMonths")) {
                payment.installmentPlanMonths = card.path("installmentPlanMonths").asInt()
            }
            if (card.has("isInterestFree")) {
                payment.interestFree = card.path("isInterestFree").asBoolean()
            }
        }

        val easy = json.path("easyPay")
        if (!easy.isMissingNode && !easy.isNull) {
            payment.easyPayProvider = easy.path("provider").asText(null)
        }

        val va = json.path("virtualAccount")
        if (!va.isMissingNode && !va.isNull) {
            payment.virtualAccountBank = va.path("bank").asText(null)
            payment.virtualAccountNumber = va.path("accountNumber").asText(null)
            payment.virtualAccountDueDate = parseDate(va.path("dueDate").asText(null))
        }

        payment.rawResponseJson = rawJson
    }

    fun applyFailure(payment: Payment, json: JsonNode?, raw: String?) {
        payment.failCode = json?.path("code")?.asText(null)
        val msg = json?.path("message")?.asText(null)
        if (!msg.isNullOrBlank()) {
            payment.failureReason = msg
        }
        if (raw != null) {
            payment.rawResponseJson = raw
        }
    }

    private fun parseDate(text: String?): LocalDateTime? {
        if (text.isNullOrBlank()) {
            return null
        }
        return try {
            OffsetDateTime.parse(text).toLocalDateTime()
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
