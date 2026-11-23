package com.sleekydz86.idolglow.global.infrastructure.logging

import ch.qos.logback.classic.spi.IThrowableProxy
import org.springframework.boot.logging.logback.ExtendedWhitespaceThrowableProxyConverter

class MaskingThrowableProxyConverter : ExtendedWhitespaceThrowableProxyConverter() {
    override fun throwableProxyToString(tp: IThrowableProxy): String {
        val text = super.throwableProxyToString(tp)
        return SensitiveLogMasker.mask(text) ?: text
    }
}
