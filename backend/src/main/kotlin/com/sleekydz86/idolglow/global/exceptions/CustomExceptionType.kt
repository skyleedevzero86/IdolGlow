package com.sleekydz86.idolglow.global.exceptions

interface CustomExceptionType {
    val errorCode: String
    val message: String
    val httpStatusCode: Int
}
