package com.sleekydz86.idolglow.airportcrowd.infrastructure

import org.springframework.http.HttpStatusCode

data class RawHttpResponse(
    val statusCode: HttpStatusCode,
    val body: String,
)
