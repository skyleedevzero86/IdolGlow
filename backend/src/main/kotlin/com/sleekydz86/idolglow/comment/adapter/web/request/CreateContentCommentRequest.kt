package com.sleekydz86.idolglow.comment.adapter.web.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "댓글·답글 작성 요청")
data class CreateContentCommentRequest(
    @field:NotBlank
    @field:Schema(description = "대상 유형. NOTICE, EVENT 는 사용할 수 없습니다.", example = "GLOW_COMMUNITY_POST")
    val targetType: String,
    @field:NotBlank
    @field:Size(max = 200)
    @field:Schema(description = "대상 키", example = "12")
    val targetKey: String,
    @field:Size(max = 300)
    @field:Schema(description = "대상 제목. 내 리뷰 목록에 보입니다.", example = "명동 맛집 추천해 주세요")
    val targetTitle: String? = null,
    @field:Size(max = 500)
    @field:Schema(description = "대상 화면 경로. / 로 시작해야 하며 /admin 은 쓸 수 없습니다.", example = "/glow?post=12")
    val targetPath: String? = null,
    @field:NotBlank
    @field:Size(max = 5000)
    @field:Schema(description = "마크다운 본문. 이미지는 먼저 올린 뒤 ![설명](url) 로 넣습니다.", example = "저도 가 봤어요!")
    val contentMarkdown: String,
    @field:Schema(description = "답글이면 부모 댓글 ID", example = "34")
    val parentId: Long? = null,
)
