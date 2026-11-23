package com.sleekydz86.idolglow.global.infrastructure.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.env.MockEnvironment

@DisplayName("로그에서 가릴 설정 값 모으기")
class SensitiveLogValueRegistrarTest {
    @DisplayName("이름에 key·secret·token·password 가 들어간 설정의 실제 값과 URL 인코딩 값을 모은다")
    @Test
    fun collectsSecretLikeValues() {
        // given
        val environment =
            MockEnvironment()
                .withProperty("kopis.service-key", "kopisValue1234")
                .withProperty("tour.api-key", "a+b/c=d12345")
                .withProperty("app.jwt.secret", "\${IDOLGLOW_TEST_UNSET_SECRET:jwtSecretValue}")
                .withProperty("spring.datasource.password", "dbPassw0rd!")
                .withProperty("app.notify.discord-webhook-url", "https://discord.test/hook/1")

        // when
        val values = SensitiveLogValueRegistrar.collectSecretValues(environment)

        // then
        assertEquals(
            setOf(
                "kopisValue1234",
                "a+b/c=d12345",
                "a%2Bb%2Fc%3Dd12345",
                "jwtSecretValue",
                "dbPassw0rd!",
                "dbPassw0rd%21",
                "https://discord.test/hook/1",
                "https%3A%2F%2Fdiscord.test%2Fhook%2F1",
            ),
            values,
        )
    }

    @DisplayName("짧은 값, 숫자만 있는 값, 참·거짓 값, 민감하지 않은 이름의 값은 모으지 않는다")
    @Test
    fun skipsValuesThatAreNotSecrets() {
        // given
        val environment =
            MockEnvironment()
                .withProperty("app.llm.max-tokens", "100000000")
                .withProperty("app.cache.key-enabled", "true")
                .withProperty("tour.service-key", "short")
                .withProperty("server.address", "localhost-long-name")

        // when
        val values = SensitiveLogValueRegistrar.collectSecretValues(environment)

        // then
        assertEquals(emptySet<String>(), values)
    }
}
