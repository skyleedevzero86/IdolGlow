package com.sleekydz86.idolglow.idolglowissue.application

data class StoreIdolGlowImageCommand(
    val bytes: ByteArray,
    val originalFilename: String?,
    val contentType: String,
    val extension: String,
    val folder: String,
)
