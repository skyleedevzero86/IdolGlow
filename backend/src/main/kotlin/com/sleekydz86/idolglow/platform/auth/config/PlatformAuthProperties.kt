package com.sleekydz86.idolglow.platform.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "platform.auth")
class PlatformAuthProperties {
    var enabled: Boolean = true
    var jwt: Jwt = Jwt()
    var password: PasswordRules = PasswordRules()
    var http: HttpAuthSettings = HttpAuthSettings()

    class Jwt {
        var secret: String = "defaultSecretKeyForDevelopmentOnly12345678901234567890"
        var logToken: Boolean = true
        var accessTokenTtl: Duration = Duration.ofHours(1)
        var refreshTokenTtl: Duration = Duration.ofDays(30)
        var recoveryTokenTtl: Duration = Duration.ofMinutes(5)
        var issuer: String = "platform"
    }

    class PasswordRules {
        var minLength: Int = 12
        var requireSpecialChars: Boolean = true
        var requireNumbers: Boolean = true
        var requireUppercase: Boolean = true
        var requireLowercase: Boolean = true
        var passwordHistoryCheckCount: Int = 5
    }

    class HttpAuthSettings {
        var accessTokenCookieName: String = "accessToken"
        var jwtFilterPermitPaths: MutableList<String> = mutableListOf(
            "/actuator/**",
            "/actuator",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/error",
            "/favicon.ico",
            "/platform/auth/login",
            "/platform/auth/register",
            "/platform/auth/refresh",
            "/platform/auth/recovery/initiate",
            "/platform/auth/recovery/reset",
        )
    }
}
