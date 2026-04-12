package com.sleekydz86.idolglow.mim.application.dto

import com.sleekydz86.idolglow.mim.domain.MimItem

data class MimAdminItemResponse(
    val imageId: String,
    val domainId: String?,
    val imageName: String?,
    val imagePath: String?,
    val imageFileName: String?,
    val description: String?,
    val activeYn: String?,
    val createdBy: String?,
    val createdAt: String?,
    val domainName: String?,
) {
    companion object {
        fun from(item: MimItem): MimAdminItemResponse =
            MimAdminItemResponse(
                imageId = item.imageId,
                domainId = item.domainId,
                imageName = item.imageName,
                imagePath = item.imagePath,
                imageFileName = item.imageFileName,
                description = item.description,
                activeYn = item.activeYn,
                createdBy = item.createdBy,
                createdAt = item.createdAtFormatted,
                domainName = item.domainName,
            )
    }
}
