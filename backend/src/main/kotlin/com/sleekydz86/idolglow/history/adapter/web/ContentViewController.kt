package com.sleekydz86.idolglow.history.adapter.web

import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import com.sleekydz86.idolglow.global.support.ContentTargetType
import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetPageResult
import com.sleekydz86.idolglow.history.adapter.web.request.RecordContentViewRequest
import com.sleekydz86.idolglow.history.application.dto.ContentViewResult
import com.sleekydz86.idolglow.history.application.dto.RecordContentViewCommand
import com.sleekydz86.idolglow.history.application.port.`in`.ContentViewUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "최근 본 콘텐츠", description = "회원별 최근 본 콘텐츠 기록 API")
@RestController
@RequestMapping("/me/content-views")
class ContentViewController(
    private val contentViewUseCase: ContentViewUseCase,
) {
    @Operation(summary = "본 콘텐츠 기록", description = "같은 콘텐츠는 한 줄만 남기고 본 시각을 갱신합니다. 회원당 최근 200개까지 보관합니다.")
    @PostMapping
    fun record(
        @LoginUser userId: Long,
        @Valid @RequestBody request: RecordContentViewRequest,
    ): ContentViewResult =
        contentViewUseCase.record(
            RecordContentViewCommand(
                userId = userId,
                contentType = ContentTargetType.parse(request.contentType),
                contentKey = request.contentKey,
                title = request.title,
                path = request.path,
                imageUrl = request.imageUrl,
            ),
        )

    @Operation(summary = "최근 본 콘텐츠 목록", description = "최근에 본 순서. page 는 0부터 시작합니다.")
    @GetMapping
    fun findMine(
        @LoginUser userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): OffsetPageResult<ContentViewResult> = contentViewUseCase.findMine(userId, OffsetPageRequest.of(page, size))

    @Operation(summary = "최근 본 콘텐츠 전체 삭제")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun clear(
        @LoginUser userId: Long,
    ) {
        contentViewUseCase.clear(userId)
    }

    @Operation(summary = "최근 본 콘텐츠 한 건 삭제", description = "본인 기록만 삭제할 수 있습니다.")
    @DeleteMapping("/{viewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @LoginUser userId: Long,
        @PathVariable viewId: Long,
    ) {
        contentViewUseCase.delete(userId, viewId)
    }
}
