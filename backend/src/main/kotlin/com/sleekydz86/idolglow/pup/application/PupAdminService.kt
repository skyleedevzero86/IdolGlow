package com.sleekydz86.idolglow.pup.application

import com.sleekydz86.idolglow.pup.application.dto.PupAdminItemResponse
import com.sleekydz86.idolglow.pup.application.dto.PupAdminPageResponse
import com.sleekydz86.idolglow.pup.application.dto.UpsertPupRequest
import com.sleekydz86.idolglow.pup.domain.PupItem
import com.sleekydz86.idolglow.pup.domain.PupListCriteria
import com.sleekydz86.idolglow.pup.domain.PupRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

@Service
@Transactional(readOnly = true)
class PupAdminService(
    private val pupRepository: PupRepository,
) {
    private val maxPopupCountPerDomain = 5

    fun findPage(
        page: Int,
        size: Int,
        searchType: String?,
        keyword: String?,
    ): PupAdminPageResponse {
        val resolvedPage = page.coerceAtLeast(1)
        val resolvedSize = size.coerceIn(1, 100)
        val resolvedSearchType = searchType?.trim()?.lowercase().orEmpty()
        val resolvedKeyword = keyword?.trim().orEmpty()
        val criteria = PupListCriteria(
            pageIndex = resolvedPage,
            pageSize = resolvedSize,
            domainId = "kr",
            searchType = resolvedSearchType,
            keyword = resolvedKeyword,
        )
        val totalCount = pupRepository.count(criteria)
        val items = pupRepository.findList(criteria)
        val totalPages = if (totalCount == 0) {
            0
        } else {
            ceil(totalCount.toDouble() / resolvedSize).toInt()
        }
        return PupAdminPageResponse(
            items = items.map(PupAdminItemResponse::from),
            page = resolvedPage,
            size = resolvedSize,
            totalElements = totalCount.toLong(),
            totalPages = totalPages,
        )
    }

    fun findOne(popupId: String): PupAdminItemResponse {
        val item = pupRepository.findById(popupId)
            ?: throw EntityNotFoundException("팝업을 찾을 수 없습니다. popupId=$popupId")
        return PupAdminItemResponse.from(item)
    }

    @Transactional
    fun create(request: UpsertPupRequest): PupAdminItemResponse {
        val resolvedDomainId = "kr"
        ensurePopupLimitNotExceeded(resolvedDomainId)
        validateNoticePeriod(request.noticeStartDate, request.noticeEndDate)

        val id = "POP_${System.currentTimeMillis()}"
        val toSave = PupItem(
            popupId = id,
            domainId = resolvedDomainId,
            title = request.title,
            fileUrl = request.fileUrl,
            linkTarget = request.linkTarget ?: "_blank",
            imagePath = request.imagePath,
            imageFileName = request.imageFileName,
            noticeStartDate = request.noticeStartDate,
            noticeEndDate = request.noticeEndDate,
            stopViewYn = request.stopViewYn ?: "Y",
            noticeYn = request.noticeYn ?: "Y",
            createdBy = request.createdBy,
            createdAtFormatted = null,
            updatedBy = null,
            updatedAtFormatted = null,
            domainName = null,
        )
        pupRepository.insert(toSave)
        return findOne(id)
    }

    @Transactional
    fun update(popupId: String, request: UpsertPupRequest): PupAdminItemResponse {
        val existing = pupRepository.findById(popupId)
            ?: throw EntityNotFoundException("팝업을 찾을 수 없습니다. popupId=$popupId")

        val nextNoticeStartDate = request.noticeStartDate ?: existing.noticeStartDate
        val nextNoticeEndDate = request.noticeEndDate ?: existing.noticeEndDate
        validateNoticePeriod(nextNoticeStartDate, nextNoticeEndDate)

        val merged = existing.copy(
            domainId = existing.domainId ?: "kr",
            title = request.title,
            fileUrl = request.fileUrl,
            linkTarget = request.linkTarget ?: existing.linkTarget,
            imagePath = request.imagePath,
            imageFileName = request.imageFileName,
            noticeStartDate = nextNoticeStartDate,
            noticeEndDate = nextNoticeEndDate,
            stopViewYn = request.stopViewYn ?: existing.stopViewYn,
            noticeYn = request.noticeYn ?: existing.noticeYn,
            updatedBy = request.updatedBy ?: request.createdBy ?: existing.updatedBy,
        )
        pupRepository.update(merged)
        return findOne(popupId)
    }

    @Transactional
    fun delete(popupId: String) {
        if (pupRepository.findById(popupId) == null) {
            throw EntityNotFoundException("팝업을 찾을 수 없습니다. popupId=$popupId")
        }
        pupRepository.delete(popupId)
    }

    private fun ensurePopupLimitNotExceeded(domainId: String) {
        val totalCount = pupRepository.count(
            PupListCriteria(
                pageIndex = 1,
                pageSize = 1,
                domainId = domainId,
                searchType = "",
                keyword = "",
            ),
        )

        if (totalCount >= maxPopupCountPerDomain) {
            throw IllegalStateException("팝업은 도메인당 최대 5개까지 등록할 수 있습니다.")
        }
    }

    private fun validateNoticePeriod(
        noticeStartDate: String?,
        noticeEndDate: String?,
    ) {
        val start = parseDateTime(noticeStartDate)
        val end = parseDateTime(noticeEndDate)

        if (start != null && end != null && start.isAfter(end)) {
            throw IllegalArgumentException("게시 종료일은 게시 시작일보다 빠를 수 없습니다.")
        }
    }

    private fun parseDateTime(raw: String?): LocalDateTime? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) {
            return null
        }

        return DATE_TIME_FORMATTERS.firstNotNullOfOrNull { formatter ->
            runCatching { LocalDateTime.parse(value, formatter) }.getOrNull()
        }
    }

    companion object {
        private val DATE_TIME_FORMATTERS = listOf(
            DateTimeFormatter.ofPattern("yyyyMMddHHmm"),
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        )
    }
}
