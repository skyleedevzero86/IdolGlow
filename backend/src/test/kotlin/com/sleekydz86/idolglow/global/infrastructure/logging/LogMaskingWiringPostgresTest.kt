package com.sleekydz86.idolglow.global.infrastructure.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.core.OutputStreamAppender
import ch.qos.logback.core.encoder.LayoutWrappingEncoder
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.slf4j.Logger.ROOT_LOGGER_NAME
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.EnumerablePropertySource

@DisplayName("실제 로그 설정의 민감정보 가리기")
class LogMaskingWiringPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var environment: ConfigurableEnvironment

    @DisplayName("콘솔 로그는 메시지와 예외 스택에 든 토큰·인증키·설정 비밀값을 가리고 나머지는 남긴다")
    @Test
    fun consoleLayoutMasksMessageAndStackTrace() {
        // given
        val configuredSecret = configuredSecretValue()
        val message = "토스 호출 실패 orderId=pay_toss_9 Authorization: Bearer live-token-123 설정값=$configuredSecret"
        val cause = IllegalStateException("연결 실패 GET https://openapi.test/rest?serviceKey=KEY-IN-STACK-777&page=1")
        val event = LoggingEvent(javaClass.name, rootLogger(), Level.ERROR, message, cause, null)

        // when
        val line = consoleLayout().doLayout(event)

        // then
        assertFalse(line.contains("live-token-123"), line)
        assertFalse(line.contains("KEY-IN-STACK-777"), line)
        assertFalse(line.contains(configuredSecret), line)
        assertTrue(line.contains("orderId=pay_toss_9"), line)
        assertTrue(line.contains("serviceKey=${SensitiveLogMasker.MASK}"), line)
        assertTrue(line.contains("IllegalStateException"), line)
    }

    private fun rootLogger(): Logger = (LoggerFactory.getILoggerFactory() as LoggerContext).getLogger(ROOT_LOGGER_NAME)

    private fun consoleLayout() =
        ((rootLogger().getAppender("CONSOLE") as OutputStreamAppender<*>).encoder as LayoutWrappingEncoder<*>)
            .layout
            .let {
                @Suppress("UNCHECKED_CAST")
                it as ch.qos.logback.core.Layout<ch.qos.logback.classic.spi.ILoggingEvent>
            }

    private fun configuredSecretValue(): String {
        val secret =
            environment.propertySources
                .filterIsInstance<EnumerablePropertySource<*>>()
                .flatMap { it.propertyNames.asList() }
                .filter { it.endsWith("secret") }
                .mapNotNull { environment.getProperty(it) }
                .firstOrNull { it.length >= MIN_LENGTH }
        assertNotNull(secret, "테스트 프로필에 8자 이상 secret 설정이 있어야 합니다")
        return secret!!
    }

    companion object {
        private const val MIN_LENGTH: Int = 8
    }
}
