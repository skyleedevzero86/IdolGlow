package com.sleekydz86.idolglow.glowcommunity.adapter.web.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "지역사회 게시글 작성·수정 요청")
data class GlowCommunityPostRequest(
    @field:NotBlank
    @field:Schema(description = "말머리. talk, question, travel", example = "talk")
    val tag: String,
    @field:NotBlank
    @field:Size(max = 150)
    @field:Schema(description = "제목", example = "명동 맛집 추천해 주세요")
    val title: String,
    @field:NotBlank
    @field:Size(max = 20000)
    @field:Schema(description = "마크다운 본문", example = "## 질문\n점심 먹을 곳을 찾고 있어요.")
    val content: String,
)
