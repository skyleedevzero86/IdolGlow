package com.sleekydz86.idolglow.sitecontent.ui

import com.sleekydz86.idolglow.sitecontent.application.SiteContentAssetQueryService
import com.sleekydz86.idolglow.sitecontent.application.dto.SiteHomeContentResponse
import com.sleekydz86.idolglow.sitecontent.application.port.`in`.SiteContentQueryUseCase
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/site-content")
class SiteContentController(
    private val siteContentQueryUseCase: SiteContentQueryUseCase,
    private val siteContentAssetQueryService: SiteContentAssetQueryService,
) : SiteContentApi {

    @GetMapping("/home")
    override fun readHomeContent(): ResponseEntity<SiteHomeContentResponse> =
        ResponseEntity.ok(siteContentQueryUseCase.readHomeContent())

    @GetMapping("/assets")
    override fun readAsset(
        @RequestParam objectKey: String,
    ): ResponseEntity<ByteArrayResource> {
        val asset = siteContentAssetQueryService.readImage(objectKey)
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
            .contentType(MediaType.parseMediaType(asset.contentType))
            .body(ByteArrayResource(asset.bytes))
    }
}
