package com.sleekydz86.idolglow.idolglowissue.application

interface IdolGlowImageStoragePort {
    fun store(command: StoreIdolGlowImageCommand): StoredIdolGlowImage
}
