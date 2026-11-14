package com.sleekydz86.idolglow.history.adapter.web.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "최근 본 콘텐츠 기록 요청")
data class RecordContentViewRequest(
    @field:NotBlank
    @field:Schema(description = "콘텐츠 유형. NOTICE, EVENT 는 기록하지 않습니다.", example = "PRODUCT")
    val contentType: String,
    @field:NotBlank
    @field:Size(max = 200)
    @field:Schema(description = "콘텐츠 키", example = "15")
    val contentKey: String,
    @field:Size(max = 300)
    @field:Schema(description = "제목. 비우면 경로를 제목으로 씁니다.", example = "성수 팝업 투어")
    val title: String? = null,
    @field:NotBlank
    @field:Size(max = 500)
    @field:Schema(description = "화면 경로. / 로 시작해야 하며 /admin 은 쓸 수 없습니다.", example = "/products/15")
    val path: String,
    @field:Schema(
        description = "대표 이미지 주소. http(s) 주소나 / 로 시작하는 경로만 저장하고 그 밖의 값은 무시합니다.",
        example = "/uploads/idolglow/cover.jpg",
    )
    val imageUrl: String? = null,
)
