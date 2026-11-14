package com.sleekydz86.idolglow.comment.adapter.web.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "댓글 수정 요청")
data class UpdateContentCommentRequest(
    @field:NotBlank
    @field:Size(max = 5000)
    @field:Schema(description = "마크다운 본문", example = "내용을 고쳤어요.")
    val contentMarkdown: String,
)
