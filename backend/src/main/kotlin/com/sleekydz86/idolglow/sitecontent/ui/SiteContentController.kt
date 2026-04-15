package com.sleekydz86.idolglow.sitecontent.ui

import com.sleekydz86.idolglow.sitecontent.application.dto.SiteHomeContentResponse
import com.sleekydz86.idolglow.sitecontent.application.port.`in`.SiteContentQueryUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/site-content")
class SiteContentController(
    private val siteContentQueryUseCase: SiteContentQueryUseCase,
) : SiteContentApi {

    @GetMapping("/home")
    override fun readHomeContent(
        @RequestParam(required = false) domainId: String?,
    ): ResponseEntity<SiteHomeContentResponse> =
        ResponseEntity.ok(siteContentQueryUseCase.readHomeContent(domainId))
}
