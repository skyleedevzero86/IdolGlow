package com.sleekydz86.idolglow.global.config

import com.sleekydz86.idolglow.global.security.JsonAccessDeniedHandler
import com.sleekydz86.idolglow.global.security.JsonAuthenticationEntryPoint
import com.sleekydz86.idolglow.global.security.JwtFilter
import com.sleekydz86.idolglow.global.security.JwtProvider
import com.sleekydz86.idolglow.user.auth.oauth.CustomOAuth2UserService
import com.sleekydz86.idolglow.user.auth.oauth.OAuth2LoginFailureHandler
import com.sleekydz86.idolglow.user.auth.oauth.OAuth2SuccessHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val jwtProvider: JwtProvider,
    private val customOAuth2UserService: CustomOAuth2UserService,
    private val oAuth2SuccessHandler: OAuth2SuccessHandler,
    private val oAuth2LoginFailureHandler: OAuth2LoginFailureHandler,
    private val authenticationEntryPoint: JsonAuthenticationEntryPoint,
    private val accessDeniedHandler: JsonAccessDeniedHandler,
    @Value("\${app.auth.test-login.enabled:false}")
    private val testLoginEnabled: Boolean,
    @Value("#{'\${app.security.allowed-origins:http://localhost:3000}'.split(',')}")
    private val allowedOrigins: List<String>,
) {

    @Bean
    fun webSecurityCustomizer(): WebSecurityCustomizer =
        WebSecurityCustomizer { web ->
            web.ignoring().requestMatchers("/", "/favicon.ico")
        }

    @Bean
    @Order(0)
    fun h2ConsoleSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/h2-console/**")
            .csrf { it.disable() }
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .headers {
                it.frameOptions { option -> option.sameOrigin() }
            }

        return http.build()
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { }
            .httpBasic { it.disable() }
            .exceptionHandling {
                it.authenticationEntryPoint(authenticationEntryPoint)
                it.accessDeniedHandler(accessDeniedHandler)
            }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/health/check").permitAll()
                auth.requestMatchers(HttpMethod.GET, "/uploads/profile-avatars/**").permitAll()
                auth.requestMatchers(*SWAGGER_WHITE_LIST).permitAll()
                val permitList = PERMIT_LIST.toMutableList()
                if (testLoginEnabled) {
                    permitList.add("/auth/test/**")
                }

                auth.requestMatchers(*permitList.toTypedArray()).permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2Login { oauth ->
                oauth.userInfoEndpoint { endpoint ->
                    endpoint.userService(customOAuth2UserService)
                }
                oauth.successHandler(oAuth2SuccessHandler)
                oauth.failureHandler(oAuth2LoginFailureHandler)
            }
            .addFilterBefore(
                JwtFilter(jwtProvider),
                UsernamePasswordAuthenticationFilter::class.java
            )

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = allowedOrigins.map { it.trim() }.filter { it.isNotEmpty() }
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        configuration.maxAge = 3600

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }

    companion object {
        private val SWAGGER_WHITE_LIST = arrayOf(
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/swagger-ui.html"
        )

        private val PERMIT_LIST = arrayOf(
            "/auth/login/**",
            "/auth/signup",
            "/auth/signup/check-email",
            "/auth/signup/check-nickname",
            "/auth/reissue",
            "/auth/logout",
            "/oauth2/**",
            "/login/oauth2/**",
            "/login",
            "/login/**",
            "/auth/callback",
            "/payments/mock/webhook",
            "/graphql",
            "/graphiql",
            "/graphiql/**",
        )
    }
}
