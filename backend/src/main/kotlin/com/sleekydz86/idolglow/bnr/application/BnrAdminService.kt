package com.sleekydz86.idolglow.bnr.application

import com.sleekydz86.idolglow.bnr.application.dto.BnrAdminItemResponse
import com.sleekydz86.idolglow.bnr.application.dto.BnrAdminPageResponse
import com.sleekydz86.idolglow.bnr.application.dto.UpsertBnrRequest
import com.sleekydz86.idolglow.bnr.domain.BnrItem
import com.sleekydz86.idolglow.bnr.domain.BnrListCriteria
import com.sleekydz86.idolglow.bnr.domain.BnrRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.ceil

@Service
@Transactional(readOnly = true)
class BnrAdminService(
    private val bnrRepository: BnrRepository,
) {
    fun findPage(
        page: Int,
        size: Int,
        searchType: String?,
        keyword: String?,
    ): BnrAdminPageResponse {
        val resolvedPage = page.coerceAtLeast(1)
        val resolvedSize = size.coerceIn(1, 100)
        val resolvedSearchType = searchType?.trim()?.lowercase().orEmpty()
        val resolvedKeyword = keyword?.trim().orEmpty()
        val criteria = BnrListCriteria(
            pageIndex = resolvedPage,
            pageSize = resolvedSize,
            domainId = "kr",
            searchType = resolvedSearchType,
            keyword = resolvedKeyword,
        )
        val totalCount = bnrRepository.count(criteria)
        val items = bnrRepository.findList(criteria)
        val totalPages = if (totalCount == 0) {
            0
        } else {
            ceil(totalCount.toDouble() / resolvedSize).toInt()
        }
        return BnrAdminPageResponse(
            items = items.map(BnrAdminItemResponse::from),
            page = resolvedPage,
            size = resolvedSize,
            totalElements = totalCount.toLong(),
            totalPages = totalPages,
        )
    }

    fun findOne(bannerId: String): BnrAdminItemResponse {
        val item = bnrRepository.findById(bannerId)
            ?: throw EntityNotFoundException("배너를 찾을 수 없습니다. bannerId=$bannerId")
        return BnrAdminItemResponse.from(item)
    }

    @Transactional
    fun create(request: UpsertBnrRequest): BnrAdminItemResponse {
        val id = "BNR_${System.currentTimeMillis()}"
        val toSave = BnrItem(
            bannerId = id,
            domainId = "kr",
            bannerName = request.bannerName,
            linkUrl = request.linkUrl,
            imagePath = request.imagePath,
            imageFileName = request.imageFileName,
            description = request.description,
            sortOrder = request.sortOrder ?: 0,
            activeYn = request.activeYn ?: "Y",
            createdBy = request.createdBy,
            createdAtFormatted = null,
            domainName = null,
        )
        bnrRepository.insert(toSave)
        return findOne(id)
    }

    @Transactional
    fun update(bannerId: String, request: UpsertBnrRequest): BnrAdminItemResponse {
        val existing = bnrRepository.findById(bannerId)
            ?: throw EntityNotFoundException("배너를 찾을 수 없습니다. bannerId=$bannerId")
        val merged = existing.copy(
            domainId = existing.domainId ?: "kr",
            bannerName = request.bannerName,
            linkUrl = request.linkUrl,
            imagePath = request.imagePath,
            imageFileName = request.imageFileName,
            description = request.description,
            sortOrder = request.sortOrder ?: existing.sortOrder,
            activeYn = request.activeYn ?: existing.activeYn,
            createdBy = request.createdBy ?: existing.createdBy,
        )
        bnrRepository.update(merged)
        return findOne(bannerId)
    }

    @Transactional
    fun delete(bannerId: String) {
        if (bnrRepository.findById(bannerId) == null) {
            throw EntityNotFoundException("배너를 찾을 수 없습니다. bannerId=$bannerId")
        }
        bnrRepository.delete(bannerId)
    }
}
