package com.sleekydz86.idolglow.global.ui

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RootController {

    @GetMapping("/")
    fun root(): Map<String, String> =
        mapOf(
            "service" to "idolglow-api",
            "health" to "/health/check",
            "swaggerUi" to "/swagger-ui.html",
            "openApi" to "/v3/api-docs",
        )

    @GetMapping("/favicon.ico")
    fun favicon(): ResponseEntity<Void> =
        ResponseEntity.status(HttpStatus.NO_CONTENT).build()
}
