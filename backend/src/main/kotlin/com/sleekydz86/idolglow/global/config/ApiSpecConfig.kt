package com.sleekydz86.idolglow.global.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ApiSpecConfig {

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title(API_TITLE)
                    .version(API_VERSION)
                    .description(API_DESCRIPTION)
            )
            .addSecurityItem(
                SecurityRequirement().addList(SECURITY_SCHEME_NAME)
            )

            .components(
                Components().addSecuritySchemes(
                    SECURITY_SCHEME_NAME,
                    SecurityScheme()
                        .name("Authorization")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                )
            )

    }

    companion object {
        private const val API_TITLE = "IdolGlow API"
        private const val API_VERSION = "0.0.1"
        private const val API_DESCRIPTION = "IdolGlow API"
        private const val SECURITY_SCHEME_NAME = "bearerAuth"
    }
}
