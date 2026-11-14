package com.sleekydz86.idolglow.history.application

import com.sleekydz86.idolglow.global.support.ContentTargetPath
import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetPageResult
import com.sleekydz86.idolglow.history.application.dto.ContentViewResult
import com.sleekydz86.idolglow.history.application.dto.RecordContentViewCommand
import com.sleekydz86.idolglow.history.application.port.`in`.ContentViewUseCase
import com.sleekydz86.idolglow.history.application.port.out.ContentViewPort
import com.sleekydz86.idolglow.history.domain.UserContentView
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class ContentViewService(
    private val contentViewPort: ContentViewPort,
) : ContentViewUseCase {
    override fun record(command: RecordContentViewCommand): ContentViewResult {
        val key = command.contentKey.trim()
        require(key.isNotEmpty()) { "콘텐츠 키를 입력해 주세요." }
        require(key.length <= UserContentView.CONTENT_KEY_MAX_LENGTH) {
            "콘텐츠 키는 ${UserContentView.CONTENT_KEY_MAX_LENGTH}자 이하여야 합니다."
        }
        val path = ContentTargetPath.requirePath(command.path)
        val title =
            command.title
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: path.take(UserContentView.TITLE_MAX_LENGTH)
        require(title.length <= UserContentView.TITLE_MAX_LENGTH) {
            "제목은 ${UserContentView.TITLE_MAX_LENGTH}자 이하여야 합니다."
        }
        val imageUrl = normalizeImageUrl(command.imageUrl)
        val now = LocalDateTime.now()
        val existing = contentViewPort.findByUserAndContent(command.userId, command.contentType, key)
        val saved =
            if (existing != null) {
                existing.viewAgain(title, path, imageUrl, now)
                contentViewPort.save(existing)
            } else {
                contentViewPort
                    .save(
                        UserContentView(
                            userId = command.userId,
                            contentType = command.contentType,
                            contentKey = key,
                            title = title,
                            path = path,
                            imageUrl = imageUrl,
                            viewedAt = now,
                        ),
                    ).also { contentViewPort.trimToLatest(command.userId, UserContentView.MAX_ROWS_PER_USER) }
            }
        return toResult(saved)
    }

    @Transactional(readOnly = true)
    override fun findMine(
        userId: Long,
        request: OffsetPageRequest,
    ): OffsetPageResult<ContentViewResult> {
        val normalized = OffsetPageRequest.of(request.page, request.size)
        val slice = contentViewPort.findByUserId(userId, normalized)
        return OffsetPageResult.of(slice.items.map(::toResult), normalized, slice.totalElements)
    }

    override fun clear(userId: Long) {
        contentViewPort.deleteAllByUserId(userId)
    }

    override fun delete(
        userId: Long,
        viewId: Long,
    ) {
        val view =
            contentViewPort.findById(viewId)?.takeIf { it.isOwnedBy(userId) }
                ?: throw EntityNotFoundException("최근 본 콘텐츠를 찾을 수 없습니다: $viewId")
        contentViewPort.delete(view)
    }

    private fun toResult(view: UserContentView): ContentViewResult =
        ContentViewResult(
            id = view.id,
            contentType = view.contentType.name,
            contentKey = view.contentKey,
            title = view.title,
            path = view.path,
            imageUrl = view.imageUrl,
            viewedAt = view.viewedAt,
        )

    private fun normalizeImageUrl(raw: String?): String? {
        val url = raw?.trim().orEmpty()
        val relative = url.startsWith("/") && !url.startsWith("//")
        val valid =
            url.isNotEmpty() &&
                url.length <= UserContentView.IMAGE_URL_MAX_LENGTH &&
                (relative || ABSOLUTE_URL.matches(url)) &&
                !UNSAFE_CHARACTERS.containsMatchIn(url)
        return url.takeIf { valid }
    }

    companion object {
        private val ABSOLUTE_URL = Regex("""^https?://[^/\s]+.*""", RegexOption.IGNORE_CASE)
        private val UNSAFE_CHARACTERS = Regex("""[\s<>"\\]""")
    }
}
