package com.sleekydz86.idolglow.productpackage.option.ui

import com.sleekydz86.idolglow.productpackage.option.application.OptionCommandService
import com.sleekydz86.idolglow.productpackage.option.application.OptionQueryService
import com.sleekydz86.idolglow.productpackage.option.application.dto.OptionImageFile
import com.sleekydz86.idolglow.productpackage.option.application.dto.OptionResponse
import com.sleekydz86.idolglow.productpackage.option.ui.request.CreateOptionRequest
import com.sleekydz86.idolglow.productpackage.option.ui.request.toCommand
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RequestMapping("/options")
@RestController
class OptionController(
    private val optionQueryService: OptionQueryService,
    private val optionCommandService: OptionCommandService,
) : OptionApi {

    @GetMapping
    override fun findOptions(): List<OptionResponse> =
        optionQueryService.findOptions()

    @GetMapping("/{optionId}")
    override fun findOption(@PathVariable optionId: Long): OptionResponse =
        optionQueryService.findOption(optionId)

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    override fun createOption(
        @Valid @RequestPart("request") request: CreateOptionRequest,
        @RequestPart("images", required = false) images: List<MultipartFile>?,
    ): OptionResponse {
        val imageFiles = OptionImageFile.from(images)
        return optionCommandService.createOption(request.toCommand(), imageFiles)
    }

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    fun createOptionJson(@Valid @RequestBody request: CreateOptionRequest): OptionResponse =
        optionCommandService.createOption(request.toCommand())

    @PutMapping("/{optionId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateOption(
        @PathVariable optionId: Long,
        @Valid @RequestBody request: CreateOptionRequest,
    ): OptionResponse =
        optionCommandService.updateOption(optionId, request.toCommand())
}
