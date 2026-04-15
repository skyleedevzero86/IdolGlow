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
        domainId: String?,
        searchType: String?,
        keyword: String?,
    ): PupAdminPageResponse {
        val resolvedPage = page.coerceAtLeast(1)
        val resolvedSize = size.coerceIn(1, 100)
        val criteria = PupListCriteria(
            pageIndex = resolvedPage,
            pageSize = resolvedSize,
            domainId = domainId?.ifBlank { null } ?: "kr",
            searchType = searchType ?: "",
            keyword = keyword ?: "",
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
            ?: throw EntityNotFoundException("Popup not found. popupId=$popupId")
        return PupAdminItemResponse.from(item)
    }

    @Transactional
    fun create(request: UpsertPupRequest): PupAdminItemResponse {
        val resolvedDomainId = request.domainId?.ifBlank { null } ?: "kr"
        ensurePopupLimitNotExceeded(resolvedDomainId)

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
            ?: throw EntityNotFoundException("Popup not found. popupId=$popupId")
        val merged = existing.copy(
            domainId = request.domainId?.ifBlank { null } ?: existing.domainId,
            title = request.title,
            fileUrl = request.fileUrl,
            linkTarget = request.linkTarget ?: existing.linkTarget,
            imagePath = request.imagePath,
            imageFileName = request.imageFileName,
            noticeStartDate = request.noticeStartDate ?: existing.noticeStartDate,
            noticeEndDate = request.noticeEndDate ?: existing.noticeEndDate,
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
            throw EntityNotFoundException("Popup not found. popupId=$popupId")
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
}
