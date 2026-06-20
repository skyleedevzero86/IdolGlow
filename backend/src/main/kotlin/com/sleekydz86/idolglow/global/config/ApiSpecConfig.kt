package com.sleekydz86.idolglow.global.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ApiSpecConfig(
    @Value("\${server.port:8080}") private val serverPort: Int,
    @Value("\${app.public-base-url:}") private val publicBaseUrl: String,
) {
    @Bean
    fun openAPI(): OpenAPI {
        val servers =
            buildList {
                if (publicBaseUrl.isNotBlank()) {
                    add(Server().url(publicBaseUrl.trimEnd('/')).description("운영/배포"))
                }
                add(Server().url("http://localhost:$serverPort").description("로컬"))
            }

        return OpenAPI()
            .info(
                Info()
                    .title(API_TITLE)
                    .version(API_VERSION)
                    .description(API_DESCRIPTION),
            ).servers(servers)
            .addSecurityItem(
                SecurityRequirement().addList(SECURITY_SCHEME_NAME),
            ).components(
                Components().addSecuritySchemes(
                    SECURITY_SCHEME_NAME,
                    SecurityScheme()
                        .name("Authorization")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("로그인·회원가입 응답의 accessToken. Swagger UI 우측 Authorize에 `Bearer {token}` 형식으로 입력."),
                ),
            )
    }

    companion object {
        private const val API_TITLE = "IdolGlow API"
        private const val API_VERSION = "1.0.0"
        private val API_DESCRIPTION =
            """
            IdolGlow 백엔드 REST API 명세입니다.

            - **인증**: 대부분의 엔드포인트는 JWT Bearer 토큰이 필요합니다. 회원가입·로그인·헬스체크·OAuth 콜백 등 일부 공개 API는 예외입니다.
            - **GraphQL**: `/graphql`, GraphiQL `/graphiql` — 조회·화면 조합용 API는 GraphQL 스키마를 참고하세요.
            - **문서**: Swagger UI `/swagger-ui/index.html`, OpenAPI JSON `/v3/api-docs`

            도메인: 상품·예약·결제, 설문·추천, 알림, 마이페이지(Glow 날씨·지하철·행사·환전·공항), 관리자 운영.
            """.trimIndent()
        private const val SECURITY_SCHEME_NAME = "bearerAuth"
    }
}
