package com.sleekydz86.idolglow.history.infrastructure

import com.sleekydz86.idolglow.global.support.ContentTargetType
import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetSlice
import com.sleekydz86.idolglow.history.application.port.out.ContentViewPort
import com.sleekydz86.idolglow.history.domain.UserContentView
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component

@Component
class ContentViewAdapter(
    private val userContentViewJpaRepository: UserContentViewJpaRepository,
) : ContentViewPort {
    override fun findByUserAndContent(
        userId: Long,
        contentType: ContentTargetType,
        contentKey: String,
    ): UserContentView? =
        userContentViewJpaRepository.findByUserIdAndContentTypeAndContentKey(userId, contentType, contentKey)

    override fun save(view: UserContentView): UserContentView = userContentViewJpaRepository.save(view)

    override fun findById(viewId: Long): UserContentView? = userContentViewJpaRepository.findById(viewId).orElse(null)

    override fun findByUserId(
        userId: Long,
        request: OffsetPageRequest,
    ): OffsetSlice<UserContentView> {
        val page =
            userContentViewJpaRepository.findByUserId(userId, PageRequest.of(request.page, request.size, NEWEST_FIRST))
        return OffsetSlice(page.content, page.totalElements)
    }

    override fun delete(view: UserContentView) = userContentViewJpaRepository.delete(view)

    override fun deleteAllByUserId(userId: Long) {
        userContentViewJpaRepository.deleteAllByUserIdInBulk(userId)
    }

    override fun trimToLatest(
        userId: Long,
        keep: Int,
    ) {
        userContentViewJpaRepository.flush()
        val overflow =
            userContentViewJpaRepository
                .findIdsByUserId(userId, PageRequest.of(0, keep + TRIM_BATCH, NEWEST_FIRST))
                .drop(keep)
        if (overflow.isNotEmpty()) userContentViewJpaRepository.deleteAllByIdInBulk(overflow)
    }

    companion object {
        private const val TRIM_BATCH = 500
        private val NEWEST_FIRST: Sort = Sort.by(Sort.Order.desc("viewedAt"), Sort.Order.desc("id"))
    }
}
