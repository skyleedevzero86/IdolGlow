package com.sleekydz86.idolglow.global.adapter.resolver

import com.sleekydz86.idolglow.global.adapter.security.AuthenticatedPrincipal
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class LoginPrincipal

@Component
class AuthenticatedPrincipalArgumentResolver(
    private val authenticatedPrincipalResolver: AuthenticatedPrincipalResolver,
) : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(LoginPrincipal::class.java) &&
            AuthenticatedPrincipal::class.java.isAssignableFrom(parameter.parameterType)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any? = authenticatedPrincipalResolver.resolveRequired()
}
