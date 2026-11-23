package com.sleekydz86.idolglow.global.infrastructure.logging

import ch.qos.logback.classic.pattern.MessageConverter
import ch.qos.logback.classic.spi.ILoggingEvent

class MaskingMessageConverter : MessageConverter() {
    override fun convert(event: ILoggingEvent): String? = SensitiveLogMasker.mask(super.convert(event))
}
