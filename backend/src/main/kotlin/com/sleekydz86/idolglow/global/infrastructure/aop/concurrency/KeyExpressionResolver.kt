package com.sleekydz86.idolglow.global.infrastructure.aop.concurrency

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext

class KeyExpressionResolver {

    private val parser = SpelExpressionParser()
    private val nameDiscoverer = DefaultParameterNameDiscoverer()

    fun resolve(expression: String, joinPoint: ProceedingJoinPoint): String {
        return try {
            val method = (joinPoint.signature as MethodSignature).method
            val paramNames = nameDiscoverer.getParameterNames(method).orEmpty()
            val context = StandardEvaluationContext(joinPoint.target)

            paramNames.forEachIndexed { index, name ->
                if (index < joinPoint.args.size) {
                    context.setVariable(name, joinPoint.args[index])
                }
            }

            val value = parser.parseExpression(expression).getValue(context)
            value?.toString() ?: "null"
        } catch (ex: Exception) {
            expression
        }
    }
}
