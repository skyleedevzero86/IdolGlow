package com.sleekydz86.idolglow.platform.user.exception

abstract class BasePlatformException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}
