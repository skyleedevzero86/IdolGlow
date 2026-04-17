package com.sleekydz86.idolglow.pup.application.dto

import com.sleekydz86.idolglow.pup.domain.PupItem

data class PupAdminItemResponse(
    val popupId: String,
    val domainId: String?,
    val title: String?,
    val fileUrl: String?,
    val linkTarget: String?,
    val imagePath: String?,
    val imageFileName: String?,
    val noticeStartDate: String?,
    val noticeEndDate: String?,
    val stopViewYn: String?,
    val noticeYn: String?,
    val createdBy: String?,
    val createdAt: String?,
    val updatedBy: String?,
    val updatedAt: String?,
    val domainName: String?,
) {
    companion object {
        fun from(item: PupItem): PupAdminItemResponse =
            PupAdminItemResponse(
                popupId = item.popupId,
                domainId = item.domainId,
                title = item.title,
                fileUrl = item.fileUrl,
                linkTarget = item.linkTarget,
                imagePath = item.imagePath,
                imageFileName = item.imageFileName,
                noticeStartDate = item.noticeStartDate,
                noticeEndDate = item.noticeEndDate,
                stopViewYn = item.stopViewYn,
                noticeYn = item.noticeYn,
                createdBy = item.createdBy,
                createdAt = item.createdAtFormatted,
                updatedBy = item.updatedBy,
                updatedAt = item.updatedAtFormatted,
                domainName = item.domainName,
            )
    }
}
