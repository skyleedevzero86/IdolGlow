package com.sleekydz86.idolglow.webzine.application

data class StoreWebzineImageCommand(
    val bytes: ByteArray,
    val originalFilename: String?,
    val contentType: String,
    val extension: String,
    val folder: String,
)
