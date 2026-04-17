package com.sleekydz86.idolglow.bnr.application.dto

import com.sleekydz86.idolglow.bnr.domain.BnrItem

data class BnrAdminItemResponse(
    val bannerId: String,
    val domainId: String?,
    val bannerName: String?,
    val linkUrl: String?,
    val imagePath: String?,
    val imageFileName: String?,
    val description: String?,
    val sortOrder: Int,
    val activeYn: String?,
    val createdBy: String?,
    val createdAt: String?,
    val domainName: String?,
) {
    companion object {
        fun from(item: BnrItem): BnrAdminItemResponse =
            BnrAdminItemResponse(
                bannerId = item.bannerId,
                domainId = item.domainId,
                bannerName = item.bannerName,
                linkUrl = item.linkUrl,
                imagePath = item.imagePath,
                imageFileName = item.imageFileName,
                description = item.description,
                sortOrder = item.sortOrder,
                activeYn = item.activeYn,
                createdBy = item.createdBy,
                createdAt = item.createdAtFormatted,
                domainName = item.domainName,
            )
    }
}
