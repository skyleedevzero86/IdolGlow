package com.sleekydz86.idolglow.sitecontent.ui

import com.sleekydz86.idolglow.sitecontent.application.dto.SiteHomeContentResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.ResponseEntity

@Tag(name = "Site content", description = "Public site content API")
interface SiteContentApi {

    @Operation(
        summary = "Read home content",
        description = "Returns home hero slides, banners, and popup content.",
    )
    @ApiResponse(responseCode = "200", description = "Read success")
    fun readHomeContent(): ResponseEntity<SiteHomeContentResponse>

    @Operation(
        summary = "Read site content asset",
        description = "Reads a banner, popup, or main slide image through the backend.",
    )
    @ApiResponse(responseCode = "200", description = "Read success")
    fun readAsset(
        @Parameter(description = "Object key", example = "webzine/site-content/popups/sample.png")
        objectKey: String,
    ): ResponseEntity<ByteArrayResource>
}
