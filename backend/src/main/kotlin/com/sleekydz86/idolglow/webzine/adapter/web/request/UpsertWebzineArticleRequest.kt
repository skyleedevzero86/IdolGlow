package com.sleekydz86.idolglow.webzine.ui.request

import com.sleekydz86.idolglow.webzine.domain.IssueCategory
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

@Schema(description = "웹진 기사 등록/수정 요청")
data class UpsertWebzineArticleRequest(
    @field:NotBlank
    @field:Size(max = 200)
    val title: String,

    @field:NotBlank
    @field:Size(max = 200)
    val kicker: String,

    @field:NotBlank
    val summary: String,

    val category: IssueCategory,

    @field:NotBlank
    @field:Size(max = 60)
    val formatLabel: String,

    @field:NotBlank
    @field:Size(max = 500)
    val heroImageUrl: String,

    @field:NotBlank
    @field:Size(max = 500)
    val cardImageUrl: String,

    val galleryImageUrls: List<@Size(max = 500) String> = emptyList(),

    val tags: List<@Size(max = 80) String> = emptyList(),

    @field:NotBlank
    @field:Size(max = 120)
    val authorName: String,

    @field:NotBlank
    @field:Email
    @field:Size(max = 255)
    val authorEmail: String,

    @field:NotBlank
    @field:Size(max = 255)
    val creditLine: String,

    @field:Size(max = 500)
    val highlightQuote: String? = null,

    @field:Valid
    @field:NotEmpty
    val sections: List<UpsertWebzineArticleSectionRequest>,
)

data class UpsertWebzineArticleSectionRequest(
    @field:Size(max = 200)
    val heading: String? = null,

    @field:NotBlank
    val body: String,

    @field:Size(max = 1000)
    val note: String? = null,
)
