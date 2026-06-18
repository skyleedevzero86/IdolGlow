package com.sleekydz86.idolglow.global.config

import com.sleekydz86.idolglow.global.adapter.resolver.LoginPrincipal
import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SpringDocConfig {
    @Bean
    fun hideInternalParametersCustomizer(): OperationCustomizer =
        OperationCustomizer { operation, handlerMethod ->
            val hiddenNames =
                handlerMethod.methodParameters
                    .filter { parameter ->
                        parameter.hasParameterAnnotation(LoginUser::class.java) ||
                            parameter.hasParameterAnnotation(LoginPrincipal::class.java) ||
                            HttpServletResponse::class.java.isAssignableFrom(parameter.parameterType) ||
                            HttpServletRequest::class.java.isAssignableFrom(parameter.parameterType)
                    }.mapNotNull { it.parameterName }
                    .toSet()

            operation.parameters?.removeIf { it.name in hiddenNames }
            operation
        }
}
