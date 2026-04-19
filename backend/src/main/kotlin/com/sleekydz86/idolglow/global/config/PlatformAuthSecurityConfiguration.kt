package com.sleekydz86.idolglow.platform.auth.config

import com.sleekydz86.idolglow.platform.auth.filter.PlatformJwtAuthenticationFilter
import com.sleekydz86.idolglow.platform.auth.filter.PlatformSecurityHeadersFilter
import com.sleekydz86.idolglow.platform.auth.handler.PlatformJwtAccessDeniedHandler
import com.sleekydz86.idolglow.platform.auth.handler.PlatformJwtAuthenticationEntryPoint
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfigurationSource
@Configuration
@ConditionalOnProperty(prefix = "platform.auth", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class PlatformAuthSecurityConfiguration(
    private val platformJwtAuthenticationFilter: PlatformJwtAuthenticationFilter,
    private val platformSecurityHeadersFilter: PlatformSecurityHeadersFilter,
    private val platformJwtAuthenticationEntryPoint: PlatformJwtAuthenticationEntryPoint,
    private val platformJwtAccessDeniedHandler: PlatformJwtAccessDeniedHandler,
    private val corsConfigurationSource: CorsConfigurationSource,
) {

    @Bean
    fun authenticationManager(authenticationConfiguration: AuthenticationConfiguration): AuthenticationManager =
        authenticationConfiguration.authenticationManager

    @Bean
    @Order(1)
    fun platformSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/platform/**")
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource) }
            .httpBasic { it.disable() }
            .exceptionHandling {
                it.authenticationEntryPoint(platformJwtAuthenticationEntryPoint)
                it.accessDeniedHandler(platformJwtAccessDeniedHandler)
            }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers(HttpMethod.OPTIONS, "/platform/**").permitAll()
                auth.requestMatchers(
                    "/platform/auth/login",
                    "/platform/auth/register",
                    "/platform/auth/refresh",
                    "/platform/auth/recovery/initiate",
                    "/platform/auth/recovery/reset",
                ).permitAll()
                auth.anyRequest().authenticated()
            }
            .addFilterBefore(platformSecurityHeadersFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(platformJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}
