package com.sleekydz86.idolglow.image.domain.domainservice

interface ImageStorage {
    fun store(
        uniqueFilename: String,
        content: ByteArray,
    ): StoredImage
}
