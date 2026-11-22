package com.sleekydz86.idolglow.comment.adapter.web

import com.sleekydz86.idolglow.comment.application.dto.MyReviewResult
import com.sleekydz86.idolglow.comment.application.port.`in`.ContentCommentUseCase
import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetPageResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "나의 리뷰", description = "회원이 남긴 모든 댓글·답글 모아 보기")
@RestController
@RequestMapping("/me/reviews")
class MyReviewController(
    private val contentCommentUseCase: ContentCommentUseCase,
) {
    @Operation(summary = "나의 리뷰 목록", description = "삭제하지 않은 내 댓글과 답글을 최신순으로 보여 줍니다. page 는 0부터 시작합니다.")
    @GetMapping
    fun findMine(
        @LoginUser userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): OffsetPageResult<MyReviewResult> = contentCommentUseCase.findMyReviews(userId, OffsetPageRequest.of(page, size))
}
