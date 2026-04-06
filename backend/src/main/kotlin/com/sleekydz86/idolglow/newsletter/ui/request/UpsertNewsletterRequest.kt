package com.sleekydz86.idolglow.newsletter.ui.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

@Schema(description = "Idol Glow 소식지 등록/수정 요청")
data class UpsertNewsletterRequest(
    @field:NotBlank
    @field:Size(max = 200)
    val title: String,

    @field:NotBlank
    @field:Size(max = 80)
    val categoryLabel: String,

    @field:NotBlank
    @field:Size(max = 10)
    val publishedAt: String,

    @field:NotBlank
    @field:Size(max = 500)
    val imageUrl: String,

    @field:NotBlank
    @field:Size(max = 2000)
    val summary: String,

    val tags: List<@Size(max = 80) String> = emptyList(),

    @field:NotEmpty
    val paragraphs: List<@Size(max = 5000) @NotBlank String>,
)
