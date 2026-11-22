package com.sleekydz86.idolglow.comment.adapter.web

import com.sleekydz86.idolglow.comment.adapter.web.request.CreateContentCommentRequest
import com.sleekydz86.idolglow.comment.adapter.web.request.UpdateContentCommentRequest
import com.sleekydz86.idolglow.comment.application.dto.CommentImageUploadResult
import com.sleekydz86.idolglow.comment.application.dto.ContentCommentResult
import com.sleekydz86.idolglow.comment.application.dto.ContentCommentTarget
import com.sleekydz86.idolglow.comment.application.dto.ContentCommentViewer
import com.sleekydz86.idolglow.comment.application.dto.CreateContentCommentCommand
import com.sleekydz86.idolglow.comment.application.port.`in`.CommentImageUploadUseCase
import com.sleekydz86.idolglow.comment.application.port.`in`.ContentCommentUseCase
import com.sleekydz86.idolglow.global.adapter.resolver.AuthenticatedPrincipalResolver
import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import com.sleekydz86.idolglow.global.support.ContentTargetType
import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetPageResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@Tag(name = "콘텐츠 댓글", description = "모든 콘텐츠 화면에서 함께 쓰는 댓글·답글 API")
@RestController
@RequestMapping("/api/comments")
class ContentCommentController(
    private val contentCommentUseCase: ContentCommentUseCase,
    private val commentImageUploadUseCase: CommentImageUploadUseCase,
    private val authenticatedPrincipalResolver: AuthenticatedPrincipalResolver,
) {
    @Operation(
        summary = "댓글 목록",
        description = "최상위 댓글을 페이지로 나누고 답글은 replies 로 최대 5단계까지 묶어 줍니다. page 는 0부터 시작합니다.",
    )
    @GetMapping
    fun findComments(
        @RequestParam targetType: String,
        @RequestParam targetKey: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "oldest") sort: String,
    ): OffsetPageResult<ContentCommentResult> =
        contentCommentUseCase.findComments(
            target = ContentCommentTarget(ContentTargetType.parse(targetType), targetKey),
            viewer = viewer(),
            request = OffsetPageRequest.of(page, size),
            latestFirst = sort.equals(LATEST, ignoreCase = true),
        )

    @Operation(summary = "댓글·답글 작성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @LoginUser userId: Long,
        @Valid @RequestBody request: CreateContentCommentRequest,
    ): ContentCommentResult =
        contentCommentUseCase.create(
            CreateContentCommentCommand(
                userId = userId,
                targetType = ContentTargetType.parse(request.targetType),
                targetKey = request.targetKey,
                targetTitle = request.targetTitle,
                targetPath = request.targetPath,
                contentMarkdown = request.contentMarkdown,
                parentId = request.parentId,
            ),
        )

    @Operation(summary = "댓글 수정", description = "본인 댓글만 수정할 수 있습니다.")
    @PutMapping("/{commentId}")
    fun update(
        @LoginUser userId: Long,
        @PathVariable commentId: Long,
        @Valid @RequestBody request: UpdateContentCommentRequest,
    ): ContentCommentResult = contentCommentUseCase.update(commentId, userId, request.contentMarkdown)

    @Operation(summary = "댓글 삭제", description = "본인 댓글 또는 관리자만 삭제할 수 있습니다. 답글이 있으면 자리만 남습니다.")
    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @LoginUser userId: Long,
        @PathVariable commentId: Long,
    ) {
        val admin = authenticatedPrincipalResolver.resolveOrNull()?.hasRole(ADMIN_ROLE) == true
        contentCommentUseCase.delete(commentId, ContentCommentViewer(userId = userId, admin = admin))
    }

    @Operation(summary = "댓글 이미지 업로드", description = "PNG, JPG, GIF, WEBP 10MB 이하. 응답 url 을 마크다운 이미지로 넣습니다.")
    @PostMapping("/images", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadImage(
        @RequestPart("file") file: MultipartFile,
    ): CommentImageUploadResult = commentImageUploadUseCase.upload(file.bytes, file.originalFilename, file.contentType)

    private fun viewer(): ContentCommentViewer {
        val principal = authenticatedPrincipalResolver.resolveOrNull() ?: return ContentCommentViewer.ANONYMOUS
        return ContentCommentViewer(userId = principal.userId, admin = principal.hasRole(ADMIN_ROLE))
    }

    companion object {
        private const val ADMIN_ROLE = "ADMIN"
        private const val LATEST = "latest"
    }
}
