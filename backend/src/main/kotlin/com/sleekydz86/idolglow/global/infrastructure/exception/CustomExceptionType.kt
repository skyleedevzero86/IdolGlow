package com.sleekydz86.idolglow.global.infrastructure.exception

interface CustomExceptionType {
    val errorCode: String
    val message: String
    val httpStatusCode: Int
}
