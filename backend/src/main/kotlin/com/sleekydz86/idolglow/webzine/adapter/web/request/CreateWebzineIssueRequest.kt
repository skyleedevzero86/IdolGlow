package com.sleekydz86.idolglow.webzine.ui.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

@Schema(description = "웹진 호 등록/수정 요청")
data class CreateWebzineIssueRequest(
    @field:Positive
    @field:Schema(description = "발행 호 번호", example = "101")
    val volume: Int,

    @field:NotBlank
    @field:Schema(description = "발행일", example = "2026.03.")
    val issueDate: String,

    @field:NotBlank
    @field:Size(max = 500)
    @field:Schema(description = "표지 이미지 URL")
    val coverImageUrl: String,

    @field:NotBlank
    @field:Size(max = 1000)
    @field:Schema(description = "호 소개 문구")
    val teaser: String,
)
