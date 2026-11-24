package com.sleekydz86.idolglow.payment.infrastructure

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class BillingKeyCryptoConverter : AttributeConverter<String, String> {
    override fun convertToDatabaseColumn(attribute: String?): String? =
        attribute?.let { BillingKeyCrypto.encrypt(it) }

    override fun convertToEntityAttribute(dbData: String?): String? =
        dbData?.let { BillingKeyCrypto.decrypt(it) }
}
