package com.sleekydz86.idolglow.global.support

import jakarta.servlet.http.HttpServletRequest

object ClientIp {
    const val UNKNOWN: String = "unknown"

    fun of(request: HttpServletRequest): String = request.remoteAddr.orEmpty().ifBlank { UNKNOWN }
}
