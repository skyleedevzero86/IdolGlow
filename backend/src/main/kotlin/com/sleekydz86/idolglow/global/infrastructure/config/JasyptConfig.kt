package com.sleekydz86.idolglow.global.infrastructure.config

import org.jasypt.encryption.StringEncryptor
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class JasyptConfig(
    private val environment: Environment
) {

    @Bean("jasyptStringEncryptor")
    fun encrypt(): StringEncryptor {
        val encryptKey =
            readDirectProperty("JASYPT_ENCRYPTOR_PASSWORD")
                ?: readDirectProperty("jasypt.encryptor.password")
                ?: readSpringProperty("jasypt.encryptor.password")
                ?: error(
                    "Jasypt 비밀번호가 없습니다. " +
                        "환경변수 JASYPT_ENCRYPTOR_PASSWORD를 설정하거나, " +
                        "JVM 옵션 -Djasypt.encryptor.password=... 를 전달하거나, " +
                        "로컬 시크릿 설정 파일에 jasypt.encryptor.password 값을 넣어주세요."
                )

        return PooledPBEStringEncryptor().apply {
            val config =
                SimpleStringPBEConfig().apply {
                    password = encryptKey
                    algorithm = "PBEWithMD5AndDES"
                    keyObtentionIterations = 1000
                    poolSize = 1
                    setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator")
                    stringOutputType = "base64"
                }
            setConfig(config)
        }
    }

    private fun readDirectProperty(name: String): String? {
        val fromEnv = System.getenv(name)?.takeUnless { it.isBlank() || isPlaceholder(it) }
        if (fromEnv != null) {
            return fromEnv
        }

        return System.getProperty(name)?.takeUnless { it.isBlank() || isPlaceholder(it) }
    }

    private fun readSpringProperty(name: String): String? =
        environment.getProperty(name)?.takeUnless { it.isBlank() || isPlaceholder(it) }

    private fun isPlaceholder(value: String): Boolean =
        value.startsWith("\${") && value.endsWith("}")
}
