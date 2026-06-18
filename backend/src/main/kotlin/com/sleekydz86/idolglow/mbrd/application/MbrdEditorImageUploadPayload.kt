package com.sleekydz86.idolglow.mbrd.application

data class MbrdEditorImageUploadPayload(
    val imageUrl: String,
    val originalFileName: String,
    val storedFileName: String,
    val size: Long,
)
