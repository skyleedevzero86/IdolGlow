package com.sleekydz86.idolglow.mim.application

import com.sleekydz86.idolglow.mim.application.dto.MimAdminItemResponse
import com.sleekydz86.idolglow.mim.application.dto.MimAdminPageResponse
import com.sleekydz86.idolglow.mim.application.dto.UpsertMimRequest
import com.sleekydz86.idolglow.mim.domain.MimItem
import com.sleekydz86.idolglow.mim.domain.MimListCriteria
import com.sleekydz86.idolglow.mim.domain.MimRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.ceil

@Service
@Transactional(readOnly = true)
class MimAdminService(
    private val mimRepository: MimRepository,
) {
    fun findPage(
        page: Int,
        size: Int,
        domainId: String?,
        searchType: String?,
        keyword: String?,
    ): MimAdminPageResponse {
        val resolvedPage = page.coerceAtLeast(1)
        val resolvedSize = size.coerceIn(1, 100)
        val criteria = MimListCriteria(
            pageIndex = resolvedPage,
            pageSize = resolvedSize,
            domainId = domainId?.ifBlank { null } ?: "kr",
            searchType = searchType ?: "",
            keyword = keyword ?: "",
        )
        val totalCount = mimRepository.count(criteria)
        val items = mimRepository.findList(criteria)
        val totalPages = if (totalCount == 0) {
            0
        } else {
            ceil(totalCount.toDouble() / resolvedSize).toInt()
        }
        return MimAdminPageResponse(
            items = items.map(MimAdminItemResponse::from),
            page = resolvedPage,
            size = resolvedSize,
            totalElements = totalCount.toLong(),
            totalPages = totalPages,
        )
    }

    fun findOne(imageId: String): MimAdminItemResponse {
        val item = mimRepository.findById(imageId)
            ?: throw EntityNotFoundException("Main image not found. imageId=$imageId")
        return MimAdminItemResponse.from(item)
    }

    @Transactional
    fun create(request: UpsertMimRequest): MimAdminItemResponse {
        val id = "IMG_${System.currentTimeMillis()}"
        val toSave = MimItem(
            imageId = id,
            domainId = request.domainId?.ifBlank { null } ?: "kr",
            imageName = request.imageName,
            imagePath = request.imagePath,
            imageFileName = request.imageFileName,
            description = request.description,
            activeYn = request.activeYn ?: "Y",
            createdBy = request.createdBy,
            createdAtFormatted = null,
            domainName = null,
        )
        mimRepository.insert(toSave)
        return findOne(id)
    }

    @Transactional
    fun update(imageId: String, request: UpsertMimRequest): MimAdminItemResponse {
        val existing = mimRepository.findById(imageId)
            ?: throw EntityNotFoundException("Main image not found. imageId=$imageId")
        val merged = existing.copy(
            domainId = request.domainId?.ifBlank { null } ?: existing.domainId,
            imageName = request.imageName,
            imagePath = request.imagePath,
            imageFileName = request.imageFileName,
            description = request.description,
            activeYn = request.activeYn ?: existing.activeYn,
            createdBy = request.createdBy ?: existing.createdBy,
        )
        mimRepository.update(merged)
        return findOne(imageId)
    }

    @Transactional
    fun delete(imageId: String) {
        if (mimRepository.findById(imageId) == null) {
            throw EntityNotFoundException("Main image not found. imageId=$imageId")
        }
        mimRepository.delete(imageId)
    }
}
