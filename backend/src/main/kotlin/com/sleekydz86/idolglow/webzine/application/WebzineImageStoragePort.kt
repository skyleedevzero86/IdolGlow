package com.sleekydz86.idolglow.webzine.application

data class StoreWebzineImageCommand(
    val bytes: ByteArray,
    val originalFilename: String?,
    val contentType: String,
    val extension: String,
    val folder: String,
)

data class StoredWebzineImage(
    val url: String,
    val objectKey: String,
    val contentType: String,
    val size: Long,
)

interface WebzineImageStoragePort {
    fun store(command: StoreWebzineImageCommand): StoredWebzineImage
}
