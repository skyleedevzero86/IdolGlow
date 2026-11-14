package com.sleekydz86.idolglow.like.adapter.web

import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import com.sleekydz86.idolglow.like.application.dto.ContentLikeResult
import com.sleekydz86.idolglow.like.application.port.`in`.ContentLikeUseCase
import com.sleekydz86.idolglow.like.domain.LikeTarget
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "좋아요", description = "트렌드 기사·행사·리뷰·댓글·지역사회 글 공용 좋아요 API")
@RestController
@RequestMapping("/api/likes")
class ContentLikeController(
    private val contentLikeUseCase: ContentLikeUseCase,
) {
    @Operation(
        summary = "좋아요",
        description =
            "targetType 은 ARTICLE, EVENT_INFO, PRODUCT_REVIEW, CONTENT_COMMENT, GLOW_COMMUNITY_POST 중 하나입니다. " +
                "targetKey 는 콘텐츠 키이며 트렌드 기사는 호슬러그/기사슬러그, 행사는 공급처:콘텐츠ID 입니다. " +
                "이미 누른 상태면 그대로 둡니다.",
    )
    @PostMapping("/{targetType}/{*targetKey}")
    fun like(
        @LoginUser userId: Long,
        @PathVariable targetType: String,
        @PathVariable targetKey: String,
    ): ContentLikeResult = contentLikeUseCase.like(userId, LikeTarget.of(targetType, targetKey.removePrefix("/")))

    @Operation(summary = "좋아요 취소", description = "누르지 않은 상태면 그대로 둡니다.")
    @DeleteMapping("/{targetType}/{*targetKey}")
    fun unlike(
        @LoginUser userId: Long,
        @PathVariable targetType: String,
        @PathVariable targetKey: String,
    ): ContentLikeResult = contentLikeUseCase.unlike(userId, LikeTarget.of(targetType, targetKey.removePrefix("/")))
}
