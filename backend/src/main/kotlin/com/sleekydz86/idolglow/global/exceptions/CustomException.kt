package com.sleekydz86.idolglow.global.exceptions

open class CustomException(
    private val customExceptionType: CustomExceptionType,
) : RuntimeException("[${customExceptionType.errorCode}]: ${customExceptionType.message}") {

    fun getExceptionType(): CustomExceptionType = customExceptionType
}
